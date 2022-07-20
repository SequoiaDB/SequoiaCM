package com.sequoiacm.infrastructure.slowlog.util;

public class ClassMetaInfo {

    private String className;

    private boolean isInterface;

    private boolean isAnnotation;

    private String superClassName;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    public boolean isAnnotation() {
        return isAnnotation;
    }

    public void setAnnotation(boolean annotation) {
        isAnnotation = annotation;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    @Override
    public String toString() {
        return "ClassMetaInfo{" + "className='" + className + '\'' + ", isInterface=" + isInterface
                + ", isAnnotation=" + isAnnotation + ", superClassName='" + superClassName + '\''
                + '}';
    }
}
