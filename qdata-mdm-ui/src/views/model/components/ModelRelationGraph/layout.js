/*
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This file is part of qData-mdm Master Data Platform (Open Source Edition).

  qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
  You may use qData-mdm for commercial purposes, but you may not remove, hide,
  modify, or replace the qData-mdm logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData-mdm as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
*/

import dagre from "dagre";

const GAP = 36;
const RANK_GAP = 88;

function layoutComponent(topology, ids, nodeWidth, nodeHeight) {
  const graph = new dagre.graphlib.Graph().setGraph({
    rankdir: "LR", ranksep: RANK_GAP, nodesep: 28, edgesep: 20,
    marginx: 16, marginy: 16,
  });
  ids.forEach(id => {
    graph.setNode(id, {width: nodeWidth, height: nodeHeight});
    topology.outEdges(id).forEach(edge => graph.setEdge(edge, {}));
  });
  dagre.layout(graph);
  return graph;
}

// 同侧端口按对端位置排序。不同模型的线分别进出，相同方向的重复字段引用沿同一路径。
function assignPorts(graph, ports) {
  for (const group of ports.values()) {
    group.sort((a, b) => a.preferredY - b.preferredY || a.peer.localeCompare(b.peer));
    const step = group.length > 1 ? Math.min(14, 72 / (group.length - 1)) : 0;
    const node = graph.node(group[0].id);
    const center = (group.length - 1) / 2;
    // 对齐最靠近卡片中心的水平线，其余端口一起错开，防止直线被挤出一个小折角。
    const preferred = group.reduce((best, p, i) => Math.abs(p.preferredY - node.y)
        < Math.abs(group[best].preferredY - node.y) ? i : best, 0);
    const margin = Math.max(0, node.height / 2 - 12 - center * step);
    const shift = Math.max(-margin, Math.min(margin, group[preferred].preferredY - node.y - (preferred - center) * step));
    group.forEach((port, index) => { port.offset = shift + (index - center) * step; });
  }
}

// 端口在卡片侧边可小幅移动。为同侧相邻端口留出间距后，优先与中间层的水平通道对齐。
function straightenPorts(graph, paths, ports) {
  function range(port) {
    const node = graph.node(port.id);
    const group = ports.get(port.id + ":" + port.side);
    const i = group.indexOf(port);
    const gap = Math.min(10, 72 / Math.max(1, group.length - 1));
    const min = i ? group[i - 1].offset + gap : -node.height / 2 + 12;
    const max = i + 1 < group.length ? group[i + 1].offset - gap : node.height / 2 - 12;
    return {min: node.y + min, max: node.y + max};
  }
  const clamp = (y, bounds) => Math.min(bounds.max, Math.max(bounds.min, y));
  for (const {start, end, points} of paths) {
    const a = range(start), b = range(end);
    const sourceY = graph.node(start.id).y, targetY = graph.node(end.id).y;
    if (points.length > 3) {
      start.offset = clamp(points[2].y, a) - sourceY;
      end.offset = clamp(points.at(-3).y, b) - targetY;
    } else if (a.min <= b.max && b.min <= a.max) {
      const y = clamp((sourceY + targetY) / 2, {min: Math.max(a.min, b.min), max: Math.min(a.max, b.max)});
      start.offset = y - sourceY;
      end.offset = y - targetY;
    }
  }
}

function routeComponent(graph, x, topology, routes) {
  const ports = new Map();
  const channels = new Map();
  const paths = [];

  function port(id, peer, side, preferredY) {
    const key = id + ":" + side;
    if (!ports.has(key)) ports.set(key, []);
    const result = {id, peer, side, preferredY, offset: 0};
    ports.get(key).push(result);
    return result;
  }
  for (const edge of graph.edges()) {
    const source = graph.node(edge.v);
    const target = graph.node(edge.w);
    const points = graph.edge(edge).points;
    if (edge.v === edge.w) {
      const route = {
        source: {cell: edge.v}, target: {cell: edge.w},
        vertices: points.slice(1, -1).map(p => ({x: x + p.x, y: p.y})),
      };
      topology.edge(edge).forEach(relation => routes.set(relation.id, route));
      continue;
    }
    const forward = target.x > source.x;
    paths.push({
      edge, points,
      start: port(edge.v, edge.w, forward ? "right" : "left", points.length > 3 ? points[2].y : target.y),
      end: port(edge.w, edge.v, forward ? "left" : "right", points.length > 3 ? points.at(-3).y : source.y),
    });
  }
  assignPorts(graph, ports);
  straightenPorts(graph, paths, ports);

  // 每隔一个 dagre 折点是跨层长边在中间层的空位。在两层之间转弯，沿预留空位穿过中间层。
  for (const path of paths) {
    const {points, start, end} = path;
    path.turns = [];
    const forward = start.side === "right";
    let level = graph.node(start.id).y + start.offset;
    for (let i = 1; i < points.length - 1; i += 2) {
      const next = i + 2 >= points.length - 1 ? graph.node(end.id).y + end.offset : points[i + 1].y;
      const lane = points[i].x;
      const turn = {lane, level, next, leftY: forward ? level : next, rightY: forward ? next : level};
      path.turns.push(turn);
      if (level !== next) {
        if (!channels.has(lane)) channels.set(lane, []);
        channels.get(lane).push(turn);
      }
      level = next;
    }
  }
  // 相交区间占用不同车道，互不相交的短线复用同一车道。
  for (const turns of channels.values()) {
    // 按实际从左到右的走向排列。向上的线从上往下分配，向下的线从下往上分配，
    // 让同向相邻的弯折依次错开，避免一条线横穿另一条线的竖段。
    turns.sort((a, b) => {
      const aDown = a.rightY > a.leftY, bDown = b.rightY > b.leftY;
      if (aDown !== bDown) return Number(aDown) - Number(bDown);
      return aDown ? b.leftY - a.leftY : a.leftY - b.leftY;
    });
    const lanes = [];
    for (const turn of turns) {
      const top = Math.min(turn.leftY, turn.rightY), bottom = Math.max(turn.leftY, turn.rightY);
      let slot = lanes.findIndex(ranges => ranges.every(r => bottom + 8 < r.top || top > r.bottom + 8));
      if (slot < 0) slot = lanes.push([]) - 1;
      lanes[slot].push({top, bottom});
      turn.slot = slot;
    }
    const spacing = Math.min(12, (RANK_GAP - 28) / Math.max(1, lanes.length - 1));
    turns.forEach(turn => { turn.lane += (turn.slot - (lanes.length - 1) / 2) * spacing; });
  }
  const terminal = p => ({
    cell: p.id, anchor: {name: p.side, args: {dy: p.offset}},
  });
  for (const {edge, start, end, turns} of paths) {
    const route = {
      source: terminal(start), target: terminal(end),
      vertices: turns.flatMap(turn => turn.level === turn.next ? [] : [
        {x: x + turn.lane, y: turn.level}, {x: x + turn.lane, y: turn.next},
      ]),
    };
    topology.edge(edge).forEach(relation => routes.set(relation.id, route));
  }
}

export function layoutRelations(nodes, edges, nodeWidth, nodeHeight) {
  const topology = new dagre.graphlib.Graph();
  nodes.forEach(node => topology.setNode(String(node.id)));
  edges.forEach(edge => {
    const source = String(edge.source), target = String(edge.target);
    if (!topology.hasNode(source) || !topology.hasNode(target)) return;
    if (!topology.hasEdge(source, target)) topology.setEdge(source, target, []);
    topology.edge(source, target).push(edge);
  });
  const positions = new Map();
  const routes = new Map();
  let x = 0;
  for (const ids of dagre.graphlib.alg.components(topology)) {
    const graph = layoutComponent(topology, ids, nodeWidth, nodeHeight);
    for (const id of ids) {
      const node = graph.node(id);
      positions.set(id, {x: x + node.x - nodeWidth / 2, y: node.y - nodeHeight / 2});
    }
    routeComponent(graph, x, topology, routes);
    x += graph.graph().width + GAP;
  }
  return {positions, routes};
}
