package tech.qiantong.qdata.mdm.security.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BizPermission {

    String code();
}
