package com.grad.code.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller 只读。
 * 使用 ThreadLocal 保证同一请求线程内可取到角色与数据范围。
 */
public class UserContext {

    /**
     * dataScope 取值：ALL=全部数据、ORG=本学院/部门、SELF=本人相关数据。
     * 需求文档第2节：学生只看本人数据，教师按本人课程，教务与管理员按授权范围。
     */
    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String dataScope) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    /** 学生：查看作业、提交代码、参与互评、发起申诉 */
    public static boolean isStudent() {
        return "STUDENT".equals(roleCode());
    }

    /** 任课教师：发布作业、登记结果、分配互评、审核成绩、处理申诉 */
    public static boolean isTeacher() {
        return "TEACHER".equals(roleCode());
    }

    /** 助教：批改互评、复核相似度、协助登记用例结果 */
    public static boolean isTa() {
        return "TA".equals(roleCode());
    }

    /** 教务管理员：维护课程与学期、审核成绩发布、导出成绩单 */
    public static boolean isAcademic() {
        return "ACADEMIC".equals(roleCode());
    }

    /** 系统管理员拥有全量数据范围 */
    public static boolean isAdmin() {
        return "ADMIN".equals(roleCode());
    }

    /** 教学管理类角色（可见他人课程与作业数据） */
    public static boolean isStaff() {
        return isTeacher() || isTa() || isAcademic() || isAdmin();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
