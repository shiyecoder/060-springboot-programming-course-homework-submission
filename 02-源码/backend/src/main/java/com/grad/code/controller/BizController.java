package com.grad.code.controller;

import com.grad.code.common.BizException;
import com.grad.code.common.R;
import com.grad.code.common.UserContext;
import com.grad.code.service.BizService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 业务接口：课程、作业、提交、互评、查重、成绩、申诉、可信度与看板。
 *
 * <p>所有写接口在服务端复核角色与数据范围，前端隐藏按钮不作为鉴权依据；
 * 列表接口统一返回 {total,page,size,records}，字段驼峰。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BizController {

    private final JdbcTemplate jdbc;
    private final BizService biz;

    private static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }

    // ============================================================ 下拉选项
    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courses", biz.rows(jdbc.queryForList(
                "select id, course_name, course_no, class_name, language_type from course "
                        + "where deleted = 0 and status <> '已归档' order by id")));
        m.put("languages", jdbc.queryForList(
                "select dict_label as label, dict_value as value from sys_dict_data "
                        + "where dict_type = 'language_type' and status = 1 order by sort_no"));
        m.put("statuses", jdbc.queryForList(
                "select dict_label as label, dict_value as value from sys_dict_data "
                        + "where dict_type = 'assignment_status' and status = 1 order by sort_no"));
        m.put("grades", jdbc.queryForList(
                "select dict_label as label, dict_value as value from sys_dict_data "
                        + "where dict_type = 'credibility_grade' and status = 1 order by sort_no"));
        m.put("appealTypes", jdbc.queryForList(
                "select dict_label as label, dict_value as value from sys_dict_data "
                        + "where dict_type = 'appeal_type' and status = 1 order by sort_no"));
        m.put("appealStatuses", jdbc.queryForList(
                "select dict_label as label, dict_value as value from sys_dict_data "
                        + "where dict_type = 'appeal_status' and status = 1 order by sort_no"));
        m.put("teachers", biz.rows(jdbc.queryForList(
                "select id, real_name, title from sys_user where role_id = 2 and deleted = 0 order by id")));
        m.put("classes", jdbc.queryForList(
                "select distinct class_name from sys_user where role_id = 1 and class_name is not null "
                        + "and class_name <> '' order by class_name"));
        return R.ok(m);
    }

    // ============================================================ 看板
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(required = false) String period) {
        Map<String, Object> m = new LinkedHashMap<>();
        // 8 个核心指标（文档第 9 节）
        m.put("pendingReview", biz.one("select count(*) from score_summary where publish_status = '未发布' "
                + "and teacher_score is not null"));
        long totalSub = biz.one("select count(*) from code_submission where deleted = 0");
        long expected = biz.one("select sum(student_count) from code_assignment a join course c on c.id = a.course_id "
                + "where a.status in ('提交中','互评中','评分中','已归档') and a.deleted = 0");
        m.put("submitRate", expected == 0 ? 0 : Math.round(totalSub * 1000.0 / expected) / 10.0);
        m.put("lateCount", biz.one("select count(*) from code_submission where is_late = 1 and deleted = 0"));
        m.put("avgPassRate", biz.dec("select round(avg(case when total_count = 0 then 0 "
                + "else pass_count * 100.0 / total_count end), 1) from code_submission "
                + "where compile_result = '通过' and deleted = 0").doubleValue());
        long taskAll = biz.one("select count(*) from peer_review_task where deleted = 0");
        long taskDone = biz.one("select count(*) from peer_review_task where status = '已评' and deleted = 0");
        m.put("peerFinishRate", taskAll == 0 ? 0 : Math.round(taskDone * 1000.0 / taskAll) / 10.0);
        m.put("lowQuality", biz.one("select count(*) from peer_review_score where low_quality = 1 and deleted = 0"));
        // 需求 5.7 + 第 9 节口径一致：告警数 = 超过阈值且仍待复核的记录数，
        // 与查重列表按「待复核」筛选的总数必须相等（否则看板 15 条、明细 9 条自相矛盾）
        m.put("similarityAlert", biz.one("select count(*) from similarity_record "
                + "where similarity_rate > 70 and review_result = '待复核' and deleted = 0"));
        m.put("pendingAppeal", biz.one("select count(*) from appeal_record where status in ('待受理','处理中') "
                + "and deleted = 0"));

        // 图 1 各作业提交率与迟交率
        m.put("submitByAssignment", biz.rows(jdbc.queryForList(
                "select a.title as name, a.id, c.course_name, c.student_count, "
                        + "count(s.id) as subs, sum(case when s.is_late = 1 then 1 else 0 end) as late, "
                        + "round(count(s.id) * 100.0 / greatest(c.student_count, 1), 1) as rate, "
                        + "round(sum(case when s.is_late = 1 then 1 else 0 end) * 100.0 / greatest(count(s.id), 1), 1) as lateRate "
                        + "from code_assignment a join course c on c.id = a.course_id "
                        + "left join code_submission s on s.assignment_id = a.id and s.deleted = 0 "
                        + "where a.status in ('提交中','互评中','评分中','已归档') and a.deleted = 0 "
                        + "group by a.id, a.title, c.course_name, c.student_count order by a.id desc limit 8")));

        // 图 2 用例通过数分布
        m.put("passDistribution", biz.rows(jdbc.queryForList(
                "select concat(bucket * 10, '-', bucket * 10 + 10, '分') as name, count(*) as value from ("
                        + "select floor(pass_count * 10 / greatest(total_count, 1)) as bucket from code_submission "
                        + "where compile_result = '通过' and deleted = 0) t group by bucket order by bucket")));

        // 图 3 互评总分与教师评分散点
        m.put("peerVsTeacher", biz.rows(jdbc.queryForList(
                "select s.peer_raw_score as peer, s.teacher_score as teacher, s.final_score, s.student_id "
                        + "from score_summary s where s.teacher_score is not null and s.peer_raw_score > 0 "
                        + "and s.deleted = 0 order by s.id limit 120")));

        // 图 4 相似度区间分布
        m.put("similarityBand", biz.rows(jdbc.queryForList(
                "select case when similarity_rate < 30 then '0-30' when similarity_rate < 50 then '30-50' "
                        + "when similarity_rate < 70 then '50-70' when similarity_rate < 85 then '70-85' "
                        + "else '85-100' end as name, count(*) as value from similarity_record "
                        + "where deleted = 0 group by 1 order by 1")));

        // 图 5 成绩分段占比
        m.put("scoreBand", biz.rows(jdbc.queryForList(
                "select case when final_score < 60 then '不及格' when final_score < 70 then '60-69' "
                        + "when final_score < 80 then '70-79' when final_score < 90 then '80-89' else '90以上' end as name, "
                        + "count(*) as value from score_summary where deleted = 0 group by 1 order by 1")));

        // 图 6 可信度等级占比
        m.put("gradeDist", biz.rows(jdbc.queryForList(
                "select grade as name, count(*) as value from credibility_snapshot where deleted = 0 group by grade")));

        // 图 7 近 6 个月提交趋势（按提交月份全量统计，不受 period 过滤影响）
        m.put("trend", biz.rows(jdbc.queryForList(
                "select date_format(submit_time, '%Y-%m') as name, count(*) as value, "
                        + "sum(case when is_late = 1 then 1 else 0 end) as late from code_submission "
                        + "where deleted = 0 and submit_time >= date_sub(curdate(), interval 6 month) "
                        + "group by 1 order by 1")));

        // 采纳率与人工修正率（创新点第 5 条）
        long snapAll = biz.one("select count(*) from credibility_snapshot where deleted = 0");
        long manual = biz.one("select count(*) from credibility_snapshot where manual_flag = 1 and deleted = 0");
        m.put("manualRate", snapAll == 0 ? 0 : Math.round(manual * 1000.0 / snapAll) / 10.0);
        m.put("snapshotTotal", snapAll);
        return R.ok(m);
    }

    // ============================================================ 课程
    @GetMapping("/course/page")
    public R<Map<String, Object>> coursePage(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String languageType) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from course c left join sys_user t on t.id = c.teacher_id "
                + "where c.deleted = 0");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (c.course_name like ? or c.course_no like ? or c.class_name like ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
            args.add(kw);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and c.status = ?");
            args.add(status);
        }
        if (languageType != null && !languageType.isBlank()) {
            where.append(" and c.language_type = ?");
            args.add(languageType);
        }
        // 教师只看本人课程
        if (UserContext.isTeacher()) {
            where.append(" and c.teacher_id = ?");
            args.add(UserContext.userId());
        }
        String select = "c.id, c.course_no, c.course_name, c.semester, c.college_name, c.class_name, "
                + "c.student_count, c.language_type, c.assignment_count, c.cover_image, c.status, "
                + "c.teacher_id, t.real_name as teacher_name, c.create_time";
        return R.ok(biz.paged(select, where.toString(), args, "c.id desc", page, size));
    }

    @PostMapping("/course")
    public R<Long> courseSave(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Object id = b.get("id");
        Long teacherId = b.get("teacherId") == null ? UserContext.userId()
                : ((Number) b.get("teacherId")).longValue();
        if (id == null) {
            String no = b.get("courseNo") == null
                    ? "KC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "0"
                    + (biz.one("select count(*) from course") % 9 + 1)
                    : str(b.get("courseNo"));
            Long nid = biz.insert("insert into course (course_no, course_name, semester, college_name, teacher_id, "
                            + "class_name, student_count, language_type, assignment_count, cover_image, status, "
                            + "deleted, version, create_by, update_by, create_time, update_time) "
                            + "values (?,?,?,?,?,?,?,?,0,?,?,0,0,?,?,now(),now())",
                    no, str(b.get("courseName")), str(b.get("semester")), str(b.get("collegeName")),
                    teacherId, str(b.get("className")), b.get("studentCount") == null ? 0 : b.get("studentCount"),
                    str(b.get("languageType")), str(b.get("coverImage")), "进行中",
                    UserContext.userId(), UserContext.userId());
            biz.log("课程管理", "新增", no, "新增课程 " + str(b.get("courseName")));
            return R.ok(nid);
        }
        biz.update("update course set course_name = coalesce(?, course_name), semester = coalesce(?, semester), "
                        + "college_name = coalesce(?, college_name), teacher_id = coalesce(?, teacher_id), "
                        + "class_name = coalesce(?, class_name), language_type = coalesce(?, language_type), "
                        + "cover_image = coalesce(?, cover_image), status = coalesce(?, status), "
                        + "update_by = ?, update_time = now() where id = ?",
                str(b.get("courseName")), str(b.get("semester")), str(b.get("collegeName")), b.get("teacherId"),
                str(b.get("className")), str(b.get("languageType")), str(b.get("coverImage")),
                str(b.get("status")), UserContext.userId(), id);
        biz.log("课程管理", "修改", String.valueOf(id), "修改课程信息");
        return R.ok(((Number) id).longValue());
    }

    // ============================================================ 作业
    @GetMapping("/assignment/page")
    public R<Map<String, Object>> assignmentPage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Long courseId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String languageType) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from code_assignment a join course c on c.id = a.course_id "
                + "left join sys_user t on t.id = c.teacher_id where a.deleted = 0");
        if (courseId != null) {
            where.append(" and a.course_id = ?");
            args.add(courseId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and a.status = ?");
            args.add(status);
        }
        if (languageType != null && !languageType.isBlank()) {
            where.append(" and a.language_type = ?");
            args.add(languageType);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (a.title like ? or a.assignment_no like ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
        }
        if (UserContext.isTeacher()) {
            where.append(" and c.teacher_id = ?");
            args.add(UserContext.userId());
        }
        String select = "a.id, a.assignment_no, a.course_id, a.title, a.language_type, a.publish_time, a.deadline, "
                + "a.late_deadline, a.review_deadline, a.test_case_count, a.peer_review_count, a.weight, a.status, "
                + "c.course_name, c.class_name, c.student_count, c.cover_image, t.real_name as teacher_name, "
                + "(select count(*) from code_submission s where s.assignment_id = a.id and s.deleted = 0) as submit_count, "
                + "(select count(*) from code_submission s where s.assignment_id = a.id and s.is_late = 1 "
                + "and s.deleted = 0) as late_count";
        return R.ok(biz.paged(select, where.toString(), args, "a.id desc", page, size));
    }

    @GetMapping("/assignment/{id}")
    public R<Map<String, Object>> assignmentDetail(@PathVariable Long id) {
        Map<String, Object> a = biz.row(jdbc.queryForList(
                "select a.*, c.course_name, c.class_name, c.student_count, c.cover_image, t.real_name as teacher_name "
                        + "from code_assignment a join course c on c.id = a.course_id "
                        + "left join sys_user t on t.id = c.teacher_id where a.id = ?", id)
                .stream().findFirst().orElse(null));
        if (a == null) {
            throw new BizException("作业不存在");
        }
        a.put("submitCount", biz.one("select count(*) from code_submission where assignment_id = ? and deleted = 0", id));
        a.put("lateCount", biz.one("select count(*) from code_submission where assignment_id = ? and is_late = 1 "
                + "and deleted = 0", id));
        a.put("avgPass", biz.dec("select round(avg(case when total_count = 0 then 0 else pass_count * 100.0 "
                + "/ total_count end), 1) from code_submission where assignment_id = ? and deleted = 0", id).doubleValue());
        // 状态流转可用动作
        List<String> next = switch (String.valueOf(a.get("status"))) {
            case "草稿" -> List.of("已发布");
            case "已发布" -> List.of("提交中", "已撤回");
            case "提交中" -> List.of("互评中");
            case "互评中" -> List.of("评分中");
            case "评分中" -> List.of("已归档");
            case "已撤回" -> List.of("已发布");
            default -> List.of();
        };
        a.put("nextActions", next);
        a.put("mySubmission", biz.row(jdbc.queryForList(
                "select s.*, u.real_name as student_name from code_submission s left join sys_user u "
                        + "on u.id = s.student_id where s.assignment_id = ? and s.student_id = ? and s.deleted = 0 "
                        + "order by s.id desc limit 1", id, UserContext.userId()).stream().findFirst().orElse(null)));
        return R.ok(a);
    }

    @PostMapping("/assignment")
    public R<Long> assignmentSave(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Object id = b.get("id");
        String title = str(b.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException("作业标题不能为空");
        }
        if (str(b.get("description")) == null || str(b.get("description")).length() < 30) {
            throw new BizException("题目描述不少于 30 字");
        }
        int cases = b.get("testCaseCount") == null ? 5 : ((Number) b.get("testCaseCount")).intValue();
        if (cases < 1 || cases > 30) {
            throw new BizException("用例数量需在 1-30 之间");
        }
        // 需求 5.6：运行结果分 / 互评分 / 教师评分 三项权重合计必须等于 100%
        int rw = b.get("runWeight") == null ? 20 : ((Number) b.get("runWeight")).intValue();
        int pw = b.get("peerWeight") == null ? 20 : ((Number) b.get("peerWeight")).intValue();
        int tw = b.get("teacherWeight") == null ? 60 : ((Number) b.get("teacherWeight")).intValue();
        if (rw < 0 || pw < 0 || tw < 0 || rw + pw + tw != 100) {
            throw new BizException("运行结果分 / 互评分 / 教师评分三项权重合计必须等于 100%，当前 "
                    + (rw + pw + tw) + "%");
        }
        // 需求 5.1：发布前校验截止时间晚于当前时间（草稿不校验）
        String status = str(b.get("status")) == null ? "草稿" : str(b.get("status"));
        LocalDateTime deadline = BizService.toTime(b.get("deadline"));
        if (!"草稿".equals(status)) {
            if (deadline == null) {
                throw new BizException("请选择截止时间");
            }
            if (!deadline.isAfter(LocalDateTime.now())) {
                throw new BizException("截止时间必须晚于当前时间");
            }
            LocalDateTime lateDl = BizService.toTime(b.get("lateDeadline"));
            if (lateDl != null && lateDl.isBefore(deadline)) {
                throw new BizException("迟交截止时间不能早于截止时间");
            }
            if (lateDl != null && lateDl.isAfter(deadline.plusDays(7))) {
                throw new BizException("迟交窗口最多 7 天");
            }
        }
        // 需求 5.1：同一课程同一周次作业标题不可重复
        List<Object> dupArgs = new ArrayList<>();
        StringBuilder dupSql = new StringBuilder("select count(*) from code_assignment where deleted = 0 "
                + "and course_id = ? and title = ? and yearweek(deadline, 1) = yearweek(?, 1)");
        dupArgs.add(((Number) b.get("courseId")).longValue());
        dupArgs.add(title);
        dupArgs.add(deadline == null ? LocalDateTime.now() : deadline);
        if (id != null) {
            dupSql.append(" and id <> ?");
            dupArgs.add(((Number) id).longValue());
        }
        if (biz.one(dupSql.toString(), dupArgs.toArray()) > 0) {
            throw new BizException("同一课程同一周次已存在同名作业《" + title + "》，请修改标题或周次");
        }
        if (id == null) {
            Long cid = ((Number) b.get("courseId")).longValue();
            String no = "ZY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                    + String.format("%03d", biz.one("select count(*) from code_assignment") + 1);
            Long nid = biz.insert("insert into code_assignment (assignment_no, course_id, title, description, "
                            + "language_type, publish_time, deadline, late_deadline, review_deadline, test_case_count, "
                            + "peer_review_count, weight, run_weight, peer_weight, teacher_weight, status, deleted, "
                            + "version, create_by, update_by, "
                            + "create_time, update_time) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,?,?,now(),now())",
                    no, cid, title, str(b.get("description")), str(b.get("languageType")),
                    "草稿".equals(status) ? null : LocalDateTime.now(),
                    b.get("deadline"), b.get("lateDeadline"), b.get("reviewDeadline"), cases,
                    b.get("peerReviewCount") == null ? 2 : b.get("peerReviewCount"),
                    b.get("weight") == null ? 10 : b.get("weight"), rw, pw, tw,
                    status, UserContext.userId(), UserContext.userId());
            biz.log("作业管理", "新增", no, "发布作业 " + title);
            return R.ok(nid);
        }
        biz.update("update code_assignment set title = ?, description = ?, language_type = ?, deadline = ?, "
                        + "late_deadline = ?, review_deadline = ?, test_case_count = ?, peer_review_count = ?, "
                        + "weight = ?, run_weight = ?, peer_weight = ?, teacher_weight = ?, "
                        + "update_by = ?, update_time = now() where id = ?",
                title, str(b.get("description")), str(b.get("languageType")), b.get("deadline"),
                b.get("lateDeadline"), b.get("reviewDeadline"), cases,
                b.get("peerReviewCount") == null ? 2 : b.get("peerReviewCount"),
                b.get("weight") == null ? 10 : b.get("weight"), rw, pw, tw, UserContext.userId(), id);
        biz.log("作业管理", "修改", String.valueOf(id), "修改作业 " + title);
        return R.ok(((Number) id).longValue());
    }

    @PostMapping("/assignment/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        biz.transition(id, str(b.get("target")), str(b.get("reason")));
        return R.ok();
    }

    // ============================================================ 提交
    @GetMapping("/submission/page")
    public R<Map<String, Object>> submissionPage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Long assignmentId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String compileResult,
                                                 @RequestParam(required = false) Integer isLate,
                                                 @RequestParam(required = false) String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from code_submission s join code_assignment a "
                + "on a.id = s.assignment_id left join sys_user u on u.id = s.student_id where s.deleted = 0");
        if (assignmentId != null) {
            where.append(" and s.assignment_id = ?");
            args.add(assignmentId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and s.status = ?");
            args.add(status);
        }
        if (compileResult != null && !compileResult.isBlank()) {
            where.append(" and s.compile_result = ?");
            args.add(compileResult);
        }
        if (isLate != null) {
            where.append(" and s.is_late = ?");
            args.add(isLate);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (u.real_name like ? or u.student_no like ? or s.submit_no like ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
            args.add(kw);
        }
        // 学生只看本人提交
        if (UserContext.isStudent()) {
            where.append(" and s.student_id = ?");
            args.add(UserContext.userId());
        } else if (UserContext.isTeacher()) {
            where.append(" and a.course_id in (select id from course where teacher_id = ?)");
            args.add(UserContext.userId());
        }
        String select = "s.id, s.submit_no, s.assignment_id, s.student_id, s.file_path, s.code_language, "
                + "s.code_lines, s.submit_time, s.submit_count, s.compile_result, s.pass_count, s.total_count, "
                + "s.error_summary, s.is_late, s.status, a.title as assignment_title, a.test_case_count, "
                + "a.course_id, u.real_name as student_name, u.student_no, u.class_name, u.avatar as student_avatar";
        return R.ok(biz.paged(select, where.toString(), args, "s.id desc", page, size));
    }

    /** 我的提交（学生端）：带作业信息与互评/成绩状态 */
    @GetMapping("/submission/mine")
    public R<Map<String, Object>> mySubmissions(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Long courseId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from code_submission s join code_assignment a "
                + "on a.id = s.assignment_id join course c on c.id = a.course_id where s.deleted = 0 "
                + "and s.student_id = ?");
        args.add(UserContext.userId());
        if (courseId != null) {
            where.append(" and a.course_id = ?");
            args.add(courseId);
        }
        String select = "s.id, s.submit_no, s.assignment_id, s.code_language, s.code_lines, s.submit_time, "
                + "s.compile_result, s.pass_count, s.total_count, s.is_late, s.status, s.file_path, "
                + "a.title as assignment_title, a.deadline, a.status as assignment_status, a.test_case_count, "
                + "c.course_name, c.cover_image, "
                + "(select count(*) from peer_review_score ps where ps.submission_id = s.id and ps.status = '已提交') as peer_count, "
                + "(select round(avg(ps.total_score),1) from peer_review_score ps where ps.submission_id = s.id) as peer_avg, "
                + "(select round(max(cs.total_score),1) from credibility_snapshot cs where cs.submission_id = s.id) as credibility";
        return R.ok(biz.paged(select, where.toString(), args, "s.id desc", page, size));
    }

    /** 学生提交代码：同一学生同一作业只保留最新有效提交（覆盖时累加提交次数） */
    @Transactional
    @PostMapping("/submission")
    public R<Map<String, Object>> submit(@RequestBody Map<String, Object> b) {
        if (!UserContext.isStudent()) {
            throw new BizException(403, "仅学生可提交代码");
        }
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        Map<String, Object> a = jdbc.queryForList(
                "select * from code_assignment where id = ? and deleted = 0", assignmentId)
                .stream().findFirst().orElseThrow(() -> new BizException("作业不存在"));
        String st = String.valueOf(a.get("status"));
        if (!List.of("提交中", "已发布", "互评中").contains(st)) {
            throw new BizException("当前作业状态（" + st + "）不接受提交");
        }
        LocalDateTime deadline = ((java.sql.Timestamp) a.get("deadline")).toLocalDateTime();
        LocalDateTime lateDl = a.get("late_deadline") == null ? deadline
                : ((java.sql.Timestamp) a.get("late_deadline")).toLocalDateTime();
        if (LocalDateTime.now().isAfter(lateDl)) {
            throw new BizException("已超过迟交截止时间，无法提交");
        }
        boolean late = LocalDateTime.now().isAfter(deadline);
        Integer exist = jdbc.queryForObject("select count(*) from code_submission where assignment_id = ? "
                + "and student_id = ? and deleted = 0", Integer.class, assignmentId, UserContext.userId());
        // ---------- 需求 5.2：提交前校验语言扩展名、单文件 200KB、代码 2000 行 ----------
        String lang = str(b.get("codeLanguage"));
        if (lang == null || lang.isBlank()) {
            lang = String.valueOf(a.get("language_type"));
        }
        String fileName = str(b.get("fileName"));
        String codeText = str(b.get("codeText"));
        if ((codeText == null || codeText.isBlank()) && (fileName == null || fileName.isBlank())) {
            throw new BizException("请粘贴代码或上传单个源文件后再提交");
        }
        Map<String, List<String>> extMap = Map.of(
                "Java", List.of(".java"), "Python", List.of(".py"),
                "C", List.of(".c", ".h"), "C++", List.of(".cpp", ".cc", ".cxx", ".hpp"),
                "JavaScript", List.of(".js"), "Go", List.of(".go"), "SQL", List.of(".sql"));
        List<String> allowExt = extMap.getOrDefault(lang, List.of(".java", ".py", ".c", ".cpp", ".js", ".go"));
        if (fileName != null && !fileName.isBlank()) {
            String lower = fileName.toLowerCase();
            int dot = lower.lastIndexOf('.');
            String ext = dot >= 0 ? lower.substring(dot) : "";
            if (ext.isEmpty() || !allowExt.contains(ext)) {
                throw new BizException("文件扩展名 " + (ext.isEmpty() ? "（无）" : ext)
                        + " 与所选语言 " + lang + " 不匹配，允许：" + String.join(" / ", allowExt));
            }
        }
        byte[] raw = codeText == null ? new byte[0] : codeText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int size = raw.length;
        Object fs = b.get("fileSize");
        if (fs instanceof Number num && num.intValue() > size) {
            size = num.intValue();
        }
        if (size > 204800) {
            throw new BizException("单个源文件不得超过 200KB（当前 " + (size / 1024) + "KB），请拆分文件或联系教师");
        }
        int lines = codeText == null || codeText.isEmpty()
                ? (b.get("codeLines") == null ? 0 : ((Number) b.get("codeLines")).intValue())
                : codeText.split("\n", -1).length;
        if (lines > 2000) {
            throw new BizException("代码行数不得超过 2000 行（当前 " + lines + " 行），请拆分文件或联系教师");
        }
        if (lines == 0) {
            throw new BizException("提交内容为空，请检查源文件");
        }
        String path = str(b.get("filePath"));
        if (path == null || path.isBlank()) {
            path = "/uploads/code/" + assignmentId + "/" + UserContext.userId()
                    + (fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".zip");
        }
        String digest = UUID.randomUUID().toString().replace("-", "");
        Long subId;
        if (exist != null && exist > 0) {
            subId = biz.one("select id from code_submission where assignment_id = ? and student_id = ? "
                    + "and deleted = 0", assignmentId, UserContext.userId());
            jdbc.update("update code_submission set file_path = ?, file_size = ?, code_text = ?, "
                            + "code_language = ?, code_lines = ?, code_digest = ?, "
                            + "submit_time = now(), submit_count = submit_count + 1, is_late = ?, "
                            + "status = '待登记', update_time = now() where id = ?",
                    path, size, codeText, lang, lines, digest, late ? 1 : 0, subId);
        } else {
            String no = "TD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                    + String.format("%05d", biz.one("select count(*) from code_submission") + 1);
            subId = biz.insert("insert into code_submission (submit_no, assignment_id, student_id, file_path, file_size, "
                            + "code_text, code_language, code_lines, code_digest, submit_time, submit_count, "
                            + "compile_result, pass_count, total_count, is_late, status, deleted, version, "
                            + "create_by, update_by, create_time, update_time) "
                            + "values (?,?,?,?,?,?,?,?,?,now(),1,'未运行',0,?,?, '待登记',0,0,?,?,now(),now())",
                    no, assignmentId, UserContext.userId(), path, size, codeText, lang, lines, digest,
                    a.get("test_case_count"), late ? 1 : 0, UserContext.userId(), UserContext.userId());
        }
        // 需求 5.2：历史版本保留可对比（每次提交写一条版本快照）
        int seq = (int) biz.one("select count(*) from code_submission_version where submission_id = ?", subId) + 1;
        Map<String, Object> subRow = jdbc.queryForList("select submit_no from code_submission where id = ?", subId)
                .stream().findFirst().orElse(Map.of("submit_no", "TD" + subId));
        biz.insert("insert into code_submission_version (version_no, submission_id, assignment_id, student_id, "
                        + "seq_no, code_language, code_lines, file_size, code_digest, code_text, is_late, "
                        + "submit_time, deleted, create_time, update_time) "
                        + "values (?,?,?,?,?,?,?,?,?,?,?,now(),0,now(),now())",
                "BB" + String.valueOf(subRow.get("submit_no")).replace("TD", "") + String.format("%02d", seq),
                subId, assignmentId, UserContext.userId(), seq, lang, lines, size, digest, codeText,
                late ? 1 : 0);
        biz.log("代码提交", "提交", String.valueOf(assignmentId),
                "学生提交代码 v" + seq + "，行数 " + lines + "，大小 " + (size / 1024) + "KB，迟交=" + late);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("late", late);
        out.put("version", seq);
        out.put("lines", lines);
        out.put("message", late ? "提交成功，已标记为迟交（第 " + seq + " 次提交）" : "提交成功（第 " + seq + " 次提交）");
        return R.ok(out);
    }

    /** 提交页上下文：作业信息 + 我的最近一次提交 + 校验规则（需求 5.2） */
    @GetMapping("/submission/context/{assignmentId}")
    public R<Map<String, Object>> submitContext(@PathVariable Long assignmentId) {
        Map<String, Object> a = biz.row(jdbc.queryForList(
                "select a.id, a.assignment_no, a.title, a.description, a.language_type, a.deadline, a.late_deadline, "
                        + "a.review_deadline, a.test_case_count, a.peer_review_count, a.run_weight, a.peer_weight, "
                        + "a.teacher_weight, a.status, c.course_name, c.class_name, c.semester, c.cover_image, "
                        + "t.real_name as teacher_name from code_assignment a join course c on c.id = a.course_id "
                        + "left join sys_user t on t.id = c.teacher_id where a.id = ?", assignmentId)
                .stream().findFirst().orElseThrow(() -> new BizException("作业不存在")));
        Map<String, Object> mine = biz.row(jdbc.queryForList(
                "select id, submit_no, code_lines, file_size, submit_count, submit_time, is_late, compile_result, "
                        + "pass_count, total_count, code_language, code_text from code_submission "
                        + "where assignment_id = ? and student_id = ? and deleted = 0 order by id desc limit 1",
                assignmentId, UserContext.userId()).stream().findFirst().orElse(null));
        a.put("mySubmission", mine);
        a.put("versions", biz.rows(jdbc.queryForList(
                "select id, version_no, seq_no, code_lines, file_size, code_language, is_late, submit_time "
                        + "from code_submission_version where assignment_id = ? and student_id = ? "
                        + "order by seq_no desc", assignmentId, UserContext.userId())));
        a.put("rules", Map.of("maxBytes", 204800, "maxLines", 2000, "maxLateDays", 7));
        a.put("extensions", Map.of("Java", ".java", "Python", ".py", "C", ".c", "C++", ".cpp",
                "JavaScript", ".js", "Go", ".go", "SQL", ".sql"));
        return R.ok(a);
    }

    /** 某个历史版本的代码正文（提交页版本对比用） */
    @GetMapping("/submission/version/{id}")
    public R<Map<String, Object>> versionDetail(@PathVariable Long id) {
        Map<String, Object> v = biz.row(jdbc.queryForList(
                "select id, version_no, submission_id, seq_no, code_language, code_lines, file_size, code_text, "
                        + "is_late, submit_time from code_submission_version where id = ?", id)
                .stream().findFirst().orElseThrow(() -> new BizException("版本不存在")));
        if (UserContext.isStudent()) {
            long owner = biz.one("select student_id from code_submission_version where id = ?", id);
            if (owner != UserContext.userId()) {
                throw new BizException(403, "只能查看本人提交的历史版本");
            }
        }
        return R.ok(v);
    }

    /** 结果登记：登记编译结果与用例通过数，可批量 */
    @Transactional
    @PostMapping("/submission/result")
    public R<Map<String, Object>> resultEntry(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Object ids = b.get("ids");
        int n = 0;
        if (ids instanceof List<?> list && !list.isEmpty()) {
            for (Object o : list) {
                Long sid = ((Number) o).longValue();
                // 需求 5.3：编译失败时通过数强制为 0
                Object pass = b.get("passCount");
                if ("编译失败".equals(str(b.get("compileResult")))) {
                    pass = 0;
                }
                jdbc.update("update code_submission set compile_result = coalesce(?, compile_result), "
                                + "pass_count = coalesce(?, pass_count), error_summary = ?, status = '已登记', "
                                + "update_time = now() where id = ?",
                        str(b.get("compileResult")), pass, str(b.get("errorSummary")), sid);
                n++;
            }
        } else {
            Long sid = ((Number) b.get("id")).longValue();
            Map<String, Object> s = jdbc.queryForList("select * from code_submission where id = ?", sid)
                    .stream().findFirst().orElseThrow(() -> new BizException("提交不存在"));
            int total = ((Number) s.get("total_count")).intValue();
            int pass = b.get("passCount") == null ? ((Number) s.get("pass_count")).intValue()
                    : ((Number) b.get("passCount")).intValue();
            if (pass > total) {
                throw new BizException("通过数不能大于用例总数 " + total);
            }
            String cr = str(b.get("compileResult")) == null ? "通过" : str(b.get("compileResult"));
            if ("编译失败".equals(cr) && (str(b.get("errorSummary")) == null
                    || str(b.get("errorSummary")).isBlank())) {
                throw new BizException("编译失败必须填写错误摘要");
            }
            // 需求 5.3：编译失败时通过数强制 0——单条登记同样要归零，不能只在校验通过数上限之前放过
            if ("编译失败".equals(cr)) {
                pass = 0;
            }
            jdbc.update("update code_submission set compile_result = ?, pass_count = ?, error_summary = ?, "
                            + "status = '已登记', update_time = now() where id = ?",
                    cr, pass, str(b.get("errorSummary")), sid);
            n = 1;
        }
        biz.log("结果登记", "登记", String.valueOf(b.get("id")), "登记运行结果 " + n + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", n);
        return R.ok(out);
    }

    /**
     * 按学号批量导入运行结果（需求 5.3）：逐行校验、逐行返回原因，绝不整批静默失败。
     * 规则：编译失败 → 通过数强制 0 且必须填写错误摘要；通过数不得超过作业用例总数；学号无提交 → 报该行原因。
     */
    @Transactional
    @PostMapping("/submission/result/import")
    public R<Map<String, Object>> resultImport(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        Map<String, Object> a = jdbc.queryForList("select test_case_count, title from code_assignment where id = ?",
                assignmentId).stream().findFirst().orElseThrow(() -> new BizException("作业不存在"));
        int total = ((Number) a.get("test_case_count")).intValue();
        Object rawRows = b.get("rows");
        if (!(rawRows instanceof List<?> rows) || rows.isEmpty()) {
            throw new BizException("导入内容为空，请检查文件");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int ok = 0;
        for (Object o : rows) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            String studentNo = str(m.get("studentNo"));
            String compileResult = str(m.get("compileResult"));
            row.put("studentNo", studentNo);
            row.put("studentName", str(m.get("studentName")));
            try {
                if (studentNo == null || studentNo.isBlank()) {
                    throw new BizException("学号为空");
                }
                if (!"通过".equals(compileResult) && !"编译失败".equals(compileResult)) {
                    throw new BizException("编译结果只能填「通过」或「编译失败」");
                }
                List<Map<String, Object>> subs = jdbc.queryForList(
                        "select s.id from code_submission s join sys_user u on u.id = s.student_id "
                                + "where s.assignment_id = ? and s.deleted = 0 and u.student_no = ? "
                                + "order by s.id desc limit 1", assignmentId, studentNo.trim());
                if (subs.isEmpty()) {
                    throw new BizException("该学号在本作业下没有提交记录");
                }
                long sid = ((Number) subs.get(0).get("id")).longValue();
                int pass = m.get("passCount") == null ? 0 : ((Number) m.get("passCount")).intValue();
                String err = str(m.get("errorSummary"));
                if ("编译失败".equals(compileResult)) {
                    if (err == null || err.isBlank()) {
                        throw new BizException("编译失败必须填写错误摘要");
                    }
                    if (pass != 0) {
                        throw new BizException("编译失败时通过数强制为 0（原填 " + pass + "）");
                    }
                }
                if (pass > total) {
                    throw new BizException("通过数 " + pass + " 超过用例总数 " + total);
                }
                if (pass < 0) {
                    throw new BizException("通过数不能为负");
                }
                jdbc.update("update code_submission set compile_result = ?, pass_count = ?, error_summary = ?, "
                        + "status = '已登记', update_time = now() where id = ?", compileResult, pass, err, sid);
                row.put("imported", true);
                row.put("reason", "");
                row.put("submissionId", sid);
                ok++;
            } catch (BizException e) {
                row.put("imported", false);
                row.put("reason", e.getMessage());
            }
            result.add(row);
        }
        biz.log("结果登记", "导入", String.valueOf(assignmentId),
                "批量导入运行结果 " + ok + "/" + result.size() + " 条成功");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", result.size());
        out.put("success", ok);
        out.put("failed", result.size() - ok);
        out.put("rows", result);
        return R.ok(out);
    }

    // ============================================================ 互评
    /** 互评工作台：我的待评/已评任务 */
    @GetMapping("/peer/task/page")
    public R<Map<String, Object>> peerTaskPage(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long assignmentId,
                                              @RequestParam(required = false) Long reviewerId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from peer_review_task t join code_submission s "
                + "on s.id = t.submission_id join code_assignment a on a.id = t.assignment_id "
                + "left join sys_user u on u.id = s.student_id where t.deleted = 0");
        if (status != null && !status.isBlank()) {
            where.append(" and t.status = ?");
            args.add(status);
        }
        if (assignmentId != null) {
            where.append(" and t.assignment_id = ?");
            args.add(assignmentId);
        }
        // 默认只看分配给本人（或指定评阅人）
        Long rv = reviewerId != null ? reviewerId : UserContext.userId();
        if (rv != null) {
            where.append(" and t.reviewer_id = ?");
            args.add(rv);
        }
        if (UserContext.isTeacher()) {
            where.append(" and a.course_id in (select id from course where teacher_id = ?)");
            args.add(UserContext.userId());
        }
        String select = "t.id, t.task_no, t.assignment_id, t.reviewer_id, t.submission_id, t.assign_round, "
                + "t.deadline, t.is_anonymous, t.finish_time, t.remind_count, t.status, "
                + "a.title as assignment_title, a.language_type, a.test_case_count, "
                + "s.code_lines, s.compile_result, s.pass_count, s.total_count, s.submit_time as submitted_at, "
                + "case when t.is_anonymous = 1 then concat('匿名同学', t.submission_id % 100) else u.real_name end as author_name, "
                + "(select count(*) from peer_review_score ps where ps.task_id = t.id and ps.status = '已提交') as scored";
        return R.ok(biz.paged(select, where.toString(), args, "t.id desc", page, size));
    }

    /** 互评工作台详情：匿名展示被评代码（需求 5.5：看不到作者姓名与学号） */
    @GetMapping("/peer/task/{id}")
    public R<Map<String, Object>> peerTaskDetail(@PathVariable Long id) {
        Map<String, Object> t = jdbc.queryForList(
                "select t.id, t.task_no, t.assignment_id, t.reviewer_id, t.submission_id, t.assign_round, t.deadline, "
                        + "t.is_anonymous, t.status, t.finish_time, t.remind_count, s.code_text, s.code_lines, "
                        + "s.code_language, s.file_size, s.submit_time as submitted_at, s.compile_result, "
                        + "s.pass_count, s.total_count, a.title as assignment_title, a.description, "
                        + "a.test_case_count, a.language_type, a.peer_weight from peer_review_task t "
                        + "join code_submission s on s.id = t.submission_id "
                        + "join code_assignment a on a.id = t.assignment_id where t.id = ? and t.deleted = 0", id)
                .stream().findFirst().orElseThrow(() -> new BizException("互评任务不存在"));
        if (UserContext.isStudent() && !UserContext.userId().equals(((Number) t.get("reviewer_id")).longValue())) {
            throw new BizException(403, "只能查看分配给本人的互评任务");
        }
        Map<String, Object> out = biz.row(t);
        if (t.get("is_anonymous") != null && ((Number) t.get("is_anonymous")).intValue() == 1) {
            out.remove("studentName");
            out.put("authorName", "匿名同学 " + (biz.one("select student_id from code_submission where id = ?",
                    t.get("submission_id")) % 100));
        }
        Map<String, Object> scored = biz.row(jdbc.queryForList(
                "select correctness_score, readability_score, style_score, total_score, comment, submit_time "
                        + "from peer_review_score where task_id = ? and status = '已提交'", id)
                .stream().findFirst().orElse(null));
        out.put("myScore", scored);
        return R.ok(out);
    }

    /** 提交互评：评分三项 + 评语（不少于 20 字），并立即计算可信度留痕 */
    @Transactional
    @PostMapping("/peer/score")
    public R<Map<String, Object>> peerScore(@RequestBody Map<String, Object> b) {
        Long taskId = ((Number) b.get("taskId")).longValue();
        Map<String, Object> t = jdbc.queryForList("select * from peer_review_task where id = ? and deleted = 0", taskId)
                .stream().findFirst().orElseThrow(() -> new BizException("互评任务不存在"));
        if (!UserContext.userId().equals(((Number) t.get("reviewer_id")).longValue())) {
            throw new BizException(403, "只能提交分配给本人的互评任务");
        }
        if (!"待评".equals(String.valueOf(t.get("status")))) {
            throw new BizException("该任务已处理（" + t.get("status") + "），不可重复提交");
        }
        double c = ((Number) b.get("correctnessScore")).doubleValue();
        double r = ((Number) b.get("readabilityScore")).doubleValue();
        double sy = ((Number) b.get("styleScore")).doubleValue();
        if (c < 0 || c > 60 || r < 0 || r > 25 || sy < 0 || sy > 15) {
            throw new BizException("分数超出范围：正确性 0-60、可读性 0-25、规范 0-15");
        }
        String comment = str(b.get("comment"));
        if (comment == null || comment.length() < 20) {
            throw new BizException("评语不少于 20 字，并指出具体代码位置");
        }
        // 需求 5.5：评语为套话先提示补充，连续两次仍提交才记入低质量评语
        boolean vague = BizService.boilerplate(comment);
        boolean force = Boolean.TRUE.equals(b.get("force")) || "1".equals(String.valueOf(b.get("force")));
        if (vague && !force) {
            throw new BizException(428, "评语未指出具体代码问题（去掉客套话后有效内容不足 15 字），"
                    + "请补充具体方法名或代码位置；如仍坚持提交，将记为低质量评语并影响可信度");
        }
        double total = BigDecimal.valueOf(c + r + sy).setScale(1, RoundingMode.HALF_UP).doubleValue();
        int lowQuality = vague ? 1 : 0;
        Long sid = biz.insert("insert into peer_review_score (task_id, reviewer_id, submission_id, "
                        + "correctness_score, readability_score, style_score, total_score, comment, comment_length, "
                        + "low_quality, submit_time, status, deleted, version, create_by, update_by, "
                        + "create_time, update_time) values (?,?,?,?,?,?,?,?,?,?,now(),'已提交',0,0,?,?,now(),now())",
                taskId, UserContext.userId(), t.get("submission_id"), c, r, sy, total, comment,
                comment.length(), lowQuality, UserContext.userId(), UserContext.userId());
        jdbc.update("update peer_review_task set status = '已评', finish_time = now(), update_time = now() "
                + "where id = ?", taskId);

        // 计算可信度并写留痕（创新点第 2 条：每次计算保存输入快照与各因子得分）
        Map<String, Object> sc = jdbc.queryForList("select s.id, s.task_id, s.reviewer_id, s.submission_id, "
                + "s.total_score, s.comment, sub.student_id, sub.assignment_id from peer_review_score s "
                + "join code_submission sub on sub.id = s.submission_id where s.id = ?", sid)
                .stream().findFirst().orElseThrow();
        Map<String, Object> cr = biz.computeCredibility(sc);
        Long snapId = biz.insert("insert into credibility_snapshot (snapshot_no, task_id, score_id, submission_id, "
                        + "student_id, assignment_id, rule_version, s_deviation, s_comment, s_finish_rate, "
                        + "s_similarity, s_consistency, total_score, grade, input_json, conclusion, manual_flag, "
                        + "calc_time, deleted, create_time, update_time) "
                        + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,now(),0,now(),now())",
                "CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                        + String.format("%06d", biz.one("select count(*) from credibility_snapshot") + 1),
                cr.get("taskId"), cr.get("scoreId"), cr.get("submissionId"), cr.get("studentId"),
                cr.get("assignmentId"), cr.get("ruleVersion"), cr.get("sDeviation"), cr.get("sComment"),
                cr.get("sFinishRate"), cr.get("sSimilarity"), cr.get("sConsistency"), cr.get("totalScore"),
                cr.get("grade"), BizService.toJson(cr), cr.get("conclusion"));
        biz.log("互评打分", "提交", String.valueOf(t.get("task_no")), "互评总分 " + total + "，可信度 " + cr.get("grade"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scoreId", sid);
        out.put("snapshotId", snapId);
        out.put("totalScore", total);
        out.put("credibility", cr);
        return R.ok(out);
    }

    /** 互评进度：按作业统计完成率、低质量数、平均分 */
    @GetMapping("/peer/progress")
    public R<List<Map<String, Object>>> peerProgress(@RequestParam(required = false) Long assignmentId) {
        List<Object> args = new ArrayList<>();
        String sql = "select a.id, a.title, a.status, c.course_name, a.peer_review_count, "
                + "count(t.id) as total, sum(case when t.status = '已评' then 1 else 0 end) as done, "
                + "sum(case when t.status = '超时未评' then 1 else 0 end) as timeout, "
                + "sum(case when t.status = '已豁免' then 1 else 0 end) as exempt, "
                + "round(sum(case when t.status = '已评' then 1 else 0 end) * 100.0 / greatest(count(t.id),1), 1) as rate, "
                + "(select count(*) from peer_review_score ps join peer_review_task t2 on t2.id = ps.task_id "
                + "where t2.assignment_id = a.id and ps.low_quality = 1) as lowQuality, "
                + "(select round(avg(ps.total_score),1) from peer_review_score ps join peer_review_task t3 "
                + "on t3.id = ps.task_id where t3.assignment_id = a.id) as avgScore "
                + "from code_assignment a join course c on c.id = a.course_id "
                + "left join peer_review_task t on t.assignment_id = a.id and t.deleted = 0 "
                + "where a.deleted = 0 and a.status in ('互评中','评分中','已归档') ";
        if (assignmentId != null) {
            sql += " and a.id = ? ";
            args.add(assignmentId);
        }
        if (UserContext.isTeacher()) {
            sql += " and c.teacher_id = ? ";
            args.add(UserContext.userId());
        }
        sql += " group by a.id, a.title, a.status, c.course_name, a.peer_review_count order by a.id desc";
        return R.ok(biz.rows(jdbc.queryForList(sql, args.toArray())));
    }

    /**
     * 互评分配的轮转方案（需求 5.4）：按提交顺序做环形错位，
     * 第 i 份提交交给排名 i+1 … i+per 的学生评，保证
     * ① 每人恰好评 per 份、每人恰好被评 per 次（偏差 0，优于「不超过 1 次」）
     * ② 绝不评自己；③ 同一份提交的评阅人互不相同。
     */
    private List<Map<String, Object>> planAssign(long assignmentId, int per) {
        List<Map<String, Object>> subs = jdbc.queryForList(
                "select s.id, s.student_id, s.submit_no, u.real_name as student_name, u.student_no, u.class_name "
                        + "from code_submission s left join sys_user u on u.id = s.student_id "
                        + "where s.assignment_id = ? and s.deleted = 0 order by s.id", assignmentId);
        int n = subs.size();
        List<Map<String, Object>> plan = new ArrayList<>();
        if (n < 2) {
            return plan;
        }
        int k = Math.min(Math.max(per, 1), n - 1);
        for (int i = 0; i < n; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            Map<String, Object> sub = subs.get(i);
            row.put("submissionId", sub.get("id"));
            row.put("submitNo", sub.get("submit_no"));
            row.put("authorId", sub.get("student_id"));
            row.put("authorName", sub.get("student_name"));
            row.put("authorNo", sub.get("student_no"));
            List<Map<String, Object>> reviewers = new ArrayList<>();
            for (int d = 1; d <= k; d++) {
                Map<String, Object> r = subs.get((i + d) % n);
                Map<String, Object> rv = new LinkedHashMap<>();
                rv.put("reviewerId", r.get("student_id"));
                rv.put("reviewerName", r.get("student_name"));
                rv.put("reviewerNo", r.get("student_no"));
                reviewers.add(rv);
            }
            row.put("reviewers", reviewers);
            plan.add(row);
        }
        return plan;
    }

    /** 分配预览：只算不写，教师确认后才落库（需求 5.4「分配结果可预览后确认」） */
    @GetMapping("/peer/assign/preview")
    public R<Map<String, Object>> peerAssignPreview(@RequestParam Long assignmentId,
                                                    @RequestParam(defaultValue = "2") int reviewCount) {
        biz.requireStaff();
        Map<String, Object> a = jdbc.queryForList("select * from code_assignment where id = ?", assignmentId)
                .stream().findFirst().orElseThrow(() -> new BizException("作业不存在"));
        List<Map<String, Object>> plan = planAssign(assignmentId, reviewCount);
        Map<Long, Integer> load = new LinkedHashMap<>();
        for (Map<String, Object> row : plan) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rs = (List<Map<String, Object>>) row.get("reviewers");
            for (Map<String, Object> r : rs) {
                load.merge(((Number) r.get("reviewerId")).longValue(), 1, Integer::sum);
            }
        }
        int min = load.isEmpty() ? 0 : Collections.min(load.values());
        int max = load.isEmpty() ? 0 : Collections.max(load.values());
        long done = biz.one("select count(*) from peer_review_task where assignment_id = ? and status = '已评' "
                + "and deleted = 0", assignmentId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assignmentId", assignmentId);
        out.put("title", a.get("title"));
        out.put("reviewCount", reviewCount);
        out.put("peerReviewCount", a.get("peer_review_count"));
        out.put("submissionCount", plan.size());
        out.put("taskTotal", plan.size() * Math.min(Math.max(reviewCount, 1),
                Math.max(plan.size() - 1, 1)));
        out.put("minPerReviewer", min);
        out.put("maxPerReviewer", max);
        out.put("deviation", max - min);
        out.put("gradedCount", done);
        out.put("locked", done > 0);
        out.put("plan", biz.rows(plan));
        return R.ok(out);
    }

    /** 互评分配：按轮转方案落库，已开始评分后只允许追加补评，不允许重排（需求 5.4） */
    @Transactional
    @PostMapping("/peer/assign")
    public R<Map<String, Object>> peerAssign(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        Map<String, Object> a = jdbc.queryForList("select * from code_assignment where id = ?", assignmentId)
                .stream().findFirst().orElseThrow(() -> new BizException("作业不存在"));
        if ("已归档".equals(String.valueOf(a.get("status")))) {
            throw new BizException("已归档作业不可重新分配互评");
        }
        int per = b.get("reviewCount") == null
                ? (int) biz.one("select coalesce(peer_review_count, 2) from code_assignment where id = ?", assignmentId)
                : ((Number) b.get("reviewCount")).intValue();
        long graded = biz.one("select count(*) from peer_review_task where assignment_id = ? and status = '已评' "
                + "and deleted = 0", assignmentId);
        if (graded > 0) {
            throw new BizException("该作业已有 " + graded + " 条互评完成，不允许重新分配；"
                    + "如仍有提交无人评，请使用「追加补评任务」");
        }
        List<Map<String, Object>> plan = planAssign(assignmentId, per);
        if (plan.isEmpty()) {
            throw new BizException("提交不足 2 份，无法分配互评");
        }
        int created = 0, skipped = 0;
        for (Map<String, Object> row : plan) {
            long subId = ((Number) row.get("submissionId")).longValue();
            long has = biz.one("select count(*) from peer_review_task where submission_id = ? and deleted = 0", subId);
            if (has > 0) {
                skipped++;
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reviewers = (List<Map<String, Object>>) row.get("reviewers");
            for (Map<String, Object> r : reviewers) {
                long reviewer = ((Number) r.get("reviewerId")).longValue();
                biz.insert("insert into peer_review_task (task_no, assignment_id, reviewer_id, submission_id, "
                                + "assign_round, deadline, is_anonymous, remind_count, status, deleted, version, "
                                + "create_by, update_by, create_time, update_time) "
                                + "values (?,?,?,?,1,?,1,0,'待评',0,0,?,?,now(),now())",
                        "HP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                                + String.format("%05d", biz.one("select count(*) from peer_review_task") + created + 1),
                        assignmentId, reviewer, subId,
                        a.get("review_deadline") == null ? a.get("deadline") : a.get("review_deadline"),
                        UserContext.userId(), UserContext.userId());
                created++;
            }
        }
        if ("提交中".equals(String.valueOf(a.get("status")))) {
            jdbc.update("update code_assignment set status = '互评中', update_time = now() where id = ?", assignmentId);
        }
        jdbc.update("update code_submission set status = '已进入互评' where assignment_id = ? and status = '已登记'",
                assignmentId);
        biz.log("互评分配", "分配", String.valueOf(assignmentId),
                "轮转分配新增互评任务 " + created + " 条，每人被评 " + per + " 次");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("created", created);
        out.put("skipped", skipped);
        out.put("perReviewer", per);
        out.put("deviation", 0);
        return R.ok(out);
    }

    /** 追加补评：给尚无任何互评任务的提交补 per 条，不动已存在的任务 */
    @Transactional
    @PostMapping("/peer/append")
    public R<Map<String, Object>> peerAppend(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        int per = b.get("reviewCount") == null ? 1 : ((Number) b.get("reviewCount")).intValue();
        Map<String, Object> a = jdbc.queryForList("select * from code_assignment where id = ?", assignmentId)
                .stream().findFirst().orElseThrow(() -> new BizException("作业不存在"));
        List<Map<String, Object>> plan = planAssign(assignmentId, per);
        int created = 0;
        for (Map<String, Object> row : plan) {
            long subId = ((Number) row.get("submissionId")).longValue();
            long has = biz.one("select count(*) from peer_review_task where submission_id = ? and deleted = 0", subId);
            if (has > 0) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reviewers = (List<Map<String, Object>>) row.get("reviewers");
            for (Map<String, Object> r : reviewers) {
                biz.insert("insert into peer_review_task (task_no, assignment_id, reviewer_id, submission_id, "
                                + "assign_round, deadline, is_anonymous, remind_count, status, deleted, version, "
                                + "create_by, update_by, create_time, update_time) "
                                + "values (?,?,?,?,2,?,1,0,'待评',0,0,?,?,now(),now())",
                        "HP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                                + String.format("%05d", biz.one("select count(*) from peer_review_task") + created + 1),
                        assignmentId, ((Number) r.get("reviewerId")).longValue(), subId,
                        a.get("review_deadline") == null ? a.get("deadline") : a.get("review_deadline"),
                        UserContext.userId(), UserContext.userId());
                created++;
            }
        }
        biz.log("互评分配", "补评", String.valueOf(assignmentId), "追加补评任务 " + created + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("created", created);
        return R.ok(out);
    }

    /** 截止前 2 小时催评（需求 5.5）：给未完成且临近截止的评阅人推送提醒并累加提醒次数 */
    @Transactional
    @PostMapping("/peer/remind")
    public R<Map<String, Object>> peerRemind(@RequestBody(required = false) Map<String, Object> b) {
        biz.requireStaff();
        Long assignmentId = b == null ? null : (b.get("assignmentId") == null ? null
                : ((Number) b.get("assignmentId")).longValue());
        List<Map<String, Object>> list = jdbc.queryForList(
                "select t.id, t.task_no, t.reviewer_id, t.deadline, a.title from peer_review_task t "
                        + "join code_assignment a on a.id = t.assignment_id "
                        + "where t.status = '待评' and t.deleted = 0 and t.remind_count < 2 "
                        + (assignmentId == null ? "" : "and t.assignment_id = ? ")
                        + "and timestampdiff(minute, now(), t.deadline) <= 120 order by t.id",
                assignmentId == null ? new Object[0] : new Object[]{assignmentId});
        int sent = 0;
        for (Map<String, Object> t : list) {
            jdbc.update("update peer_review_task set remind_count = remind_count + 1, update_time = now() where id = ?",
                    t.get("id"));
            biz.insert("insert into sys_message (title, content, msg_type, receiver_id, read_flag, create_time, "
                            + "update_time) values (?,?,?,?,0,now(),now())",
                    "互评即将截止", "你有一条互评任务将于 2 小时内截止：" + t.get("title") + "，任务号 " + t.get("task_no"),
                    "互评提醒", t.get("reviewer_id"));
            sent++;
        }
        biz.log("互评打分", "催评", assignmentId == null ? "-" : String.valueOf(assignmentId),
                "截止前 2 小时提醒 " + sent + " 人次");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sent", sent);
        return R.ok(out);
    }

    // ============================================================ 查重
    @GetMapping("/similarity/page")
    public R<Map<String, Object>> similarityPage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Long assignmentId,
                                                 @RequestParam(required = false) String reviewResult,
                                                 @RequestParam(required = false) Double minRate) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from similarity_record r join code_assignment a "
                + "on a.id = r.assignment_id left join sys_user ua on ua.id = "
                + "(select student_id from code_submission where id = r.submission_a_id) "
                + "left join sys_user ub on ub.id = (select student_id from code_submission where id = r.submission_b_id) "
                + "where r.deleted = 0");
        if (assignmentId != null) {
            where.append(" and r.assignment_id = ?");
            args.add(assignmentId);
        }
        if (reviewResult != null && !reviewResult.isBlank()) {
            where.append(" and r.review_result = ?");
            args.add(reviewResult);
        }
        if (minRate != null) {
            where.append(" and r.similarity_rate >= ?");
            args.add(minRate);
        }
        String select = "r.id, r.record_no, r.assignment_id, r.submission_a_id, r.submission_b_id, "
                + "r.similarity_rate, r.structure_rate, r.identifier_rate, r.comment_rate, r.match_type, "
                + "r.evidence_snippet, r.evidence_image, r.check_rule_version, r.batch_no, r.review_result, "
                + "r.reviewer_id, r.second_reviewer_id, r.review_remark, r.review_time, r.create_time, "
                + "a.title as assignment_title, a.language_type, ua.real_name as student_a, ub.real_name as student_b, "
                + "ua.student_no as no_a, ub.student_no as no_b";
        return R.ok(biz.paged(select, where.toString(), args, "r.similarity_rate desc", page, size));
    }

    /** 相似度复核：确认抄袭必须两名教师签字 */
    @Transactional
    @PostMapping("/similarity/{id}/review")
    public R<Void> similarityReview(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        String result = str(b.get("reviewResult"));
        if (!List.of("确认抄袭", "误报", "需面谈").contains(result)) {
            throw new BizException("复核结论只能是 确认抄袭/误报/需面谈");
        }
        String remark = str(b.get("reviewRemark"));
        if (remark == null || remark.length() < 5) {
            throw new BizException("复核理由不少于 5 字");
        }
        Object second = b.get("secondReviewerId");
        if ("确认抄袭".equals(result) && second == null) {
            throw new BizException("确认抄袭需第二名教师签字");
        }
        jdbc.update("update similarity_record set review_result = ?, reviewer_id = ?, second_reviewer_id = ?, "
                        + "review_remark = ?, review_time = now(), update_time = now() where id = ?",
                result, UserContext.userId(), second, remark, id);
        biz.log("相似度复核", "审核", String.valueOf(id), "复核结论 " + result);
        return R.ok();
    }

    /** 代码相似度检测：按结构/标识符/注释合成，超过告警线标记待复核 */
    @Transactional
    @PostMapping("/similarity/check")
    public R<Map<String, Object>> similarityCheck(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        Map<String, Object> rule = biz.currentRule();
        double alert = BizService.num(rule.get("similarity_alert"), 70);
        List<Map<String, Object>> subs = jdbc.queryForList(
                "select id, code_digest, code_lines from code_submission where assignment_id = ? and deleted = 0",
                assignmentId);
        int n = 0;
        String batch = "PC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        for (int i = 0; i < subs.size(); i++) {
            for (int j = i + 1; j < subs.size(); j++) {
                Map<String, Object> x = subs.get(i), y = subs.get(j);
                long a = ((Number) x.get("id")).longValue(), bb = ((Number) y.get("id")).longValue();
                long dup = biz.one("select count(*) from similarity_record where assignment_id = ? "
                        + "and submission_a_id = ? and submission_b_id = ?", assignmentId, Math.min(a, bb), Math.max(a, bb));
                if (dup > 0) {
                    continue;
                }
                // 结构按代码行数接近度，标识符按指纹一致，注释按随机稳定散列（真实工程需 AST 比对）
                int la = ((Number) x.get("code_lines")).intValue(), lb = ((Number) y.get("code_lines")).intValue();
                double st = BizService.clamp(100 - Math.abs(la - lb) * 100.0 / Math.max(la, Math.max(lb, 1)));
                boolean sameDigest = x.get("code_digest") != null && x.get("code_digest").equals(y.get("code_digest"));
                double idr = sameDigest ? 96 : BizService.clamp(st * 0.82 + new Random(a * 31 + bb).nextInt(20));
                double cm = BizService.clamp(idr * 0.55 + new Random(a + bb * 17).nextInt(30));
                double total = BizService.similarityTotal(st, idr, cm);
                biz.insert("insert into similarity_record (record_no, assignment_id, submission_a_id, submission_b_id, "
                                + "similarity_rate, structure_rate, identifier_rate, comment_rate, match_type, "
                                + "evidence_snippet, check_rule_version, batch_no, review_result, deleted, version, "
                                + "create_by, update_by, create_time, update_time) "
                                + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,?,?,now(),now())",
                        "CD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                                + String.format("%04d", biz.one("select count(*) from similarity_record") + n + 1),
                        assignmentId, Math.min(a, bb), Math.max(a, bb), total, st, idr, cm,
                        sameDigest ? "完全一致" : (idr < st ? "变量重命名" : "结构一致"),
                        "结构相似 " + String.format("%.1f", st) + "%，标识符相似 " + String.format("%.1f", idr) + "%",
                        "v1.2", batch, total > alert ? "待复核" : "误报",
                        UserContext.userId(), UserContext.userId());
                n++;
            }
        }
        biz.log("查重", "检测", batch, "新增查重记录 " + n + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", n);
        out.put("batchNo", batch);
        out.put("alertLine", alert);
        return R.ok(out);
    }

    // ============================================================ 成绩
    @GetMapping("/score/page")
    public R<Map<String, Object>> scorePage(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Long assignmentId,
                                            @RequestParam(required = false) String publishStatus,
                                            @RequestParam(required = false) String grade,
                                            @RequestParam(required = false) String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from score_summary s join code_assignment a "
                + "on a.id = s.assignment_id join course c on c.id = a.course_id "
                + "left join sys_user u on u.id = s.student_id where s.deleted = 0");
        if (assignmentId != null) {
            where.append(" and s.assignment_id = ?");
            args.add(assignmentId);
        }
        if (publishStatus != null && !publishStatus.isBlank()) {
            where.append(" and s.publish_status = ?");
            args.add(publishStatus);
        }
        if (grade != null && !grade.isBlank()) {
            where.append(" and s.credibility_grade = ?");
            args.add(grade);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (u.real_name like ? or u.student_no like ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
        }
        if (UserContext.isStudent()) {
            where.append(" and s.student_id = ?");
            args.add(UserContext.userId());
        } else if (UserContext.isTeacher()) {
            where.append(" and c.teacher_id = ?");
            args.add(UserContext.userId());
        }
        String select = "s.id, s.assignment_id, s.student_id, s.auto_score, s.peer_score, s.peer_raw_score, "
                + "s.teacher_score, s.final_score, s.class_rank, s.credibility_score, s.credibility_grade, "
                + "s.adjust_reason, s.publish_status, s.publish_time, s.create_time, "
                + "a.title as assignment_title, a.weight, a.run_weight, a.peer_weight, a.teacher_weight, "
                + "c.course_name, u.real_name as student_name, "
                + "u.student_no, u.class_name, u.avatar as student_avatar";
        return R.ok(biz.paged(select, where.toString(), args, "s.id desc", page, size));
    }

    /** 成绩合成：按可信度等级决定互评分如何计入（创新点第 3 条） */
    @Transactional
    @PostMapping("/score/compose")
    public R<Map<String, Object>> scoreCompose(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        Map<String, Object> asg = jdbc.queryForList("select run_weight, peer_weight, teacher_weight, title "
                + "from code_assignment where id = ?", assignmentId).stream().findFirst()
                .orElseThrow(() -> new BizException("作业不存在"));
        // 需求 5.6：权重合计必须等于 100，缺失时回落到 20/20/60
        double wr = BizService.num(asg.get("run_weight"), 20);
        double wp = BizService.num(asg.get("peer_weight"), 20);
        double wt = BizService.num(asg.get("teacher_weight"), 60);
        if (Math.abs(wr + wp + wt - 100) > 0.01) {
            throw new BizException("作业《" + asg.get("title") + "》三项权重合计为 "
                    + (int) (wr + wp + wt) + "%，请先在作业设置中调整为 100% 再合成");
        }
        List<Map<String, Object>> subs = jdbc.queryForList(
                "select id, student_id, pass_count, total_count from code_submission "
                        + "where assignment_id = ? and deleted = 0", assignmentId);
        int n = 0;
        for (Map<String, Object> s : subs) {
            long subId = ((Number) s.get("id")).longValue();
            BigDecimal auto = BigDecimal.valueOf(BizService.num(s.get("pass_count"), 0) * 100
                    / Math.max(1, BizService.num(s.get("total_count"), 1)));
            BigDecimal peerRaw = biz.dec("select avg(total_score) from peer_review_score "
                    + "where submission_id = ? and status = '已提交'", subId);
            BigDecimal cred = biz.dec("select max(total_score) from credibility_snapshot where submission_id = ?", subId);
            String grade = cred.doubleValue() <= 0 ? "中可信" : biz.gradeOf(cred.doubleValue(), biz.currentRule());
            double factor = switch (grade) {
                case "高可信" -> 1.0;
                case "中可信" -> 0.8;
                default -> 0.0;
            };
            BigDecimal peer = peerRaw.multiply(BigDecimal.valueOf(factor)).setScale(1, RoundingMode.HALF_UP);
            BigDecimal teacher = biz.dec("select teacher_score from score_summary where assignment_id = ? "
                    + "and student_id = ?", assignmentId, s.get("student_id"));
            // 缺项时按剩余项归一化，避免学生因「教师未评分」直接丢分
            double wR = wr, wP = peerRaw.doubleValue() > 0 ? wp : 0, wT = teacher.doubleValue() > 0 ? wt : 0;
            double base = wR + wP + wT;
            if (base <= 0) {
                base = 100;
                wR = 100;
            }
            double finalScore = (auto.doubleValue() * wR + peer.doubleValue() * wP + teacher.doubleValue() * wT) / base;
            BigDecimal fin = BigDecimal.valueOf(finalScore).setScale(1, RoundingMode.HALF_UP);
            Long exist = biz.one("select id from score_summary where assignment_id = ? and student_id = ?",
                    assignmentId, s.get("student_id"));
            if (exist > 0) {
                jdbc.update("update score_summary set auto_score = ?, peer_score = ?, peer_raw_score = ?, "
                                + "final_score = ?, credibility_score = ?, credibility_grade = ?, update_time = now() "
                                + "where id = ?", auto, peer, peerRaw, fin, cred, grade, exist);
            } else {
                biz.insert("insert into score_summary (assignment_id, student_id, auto_score, peer_score, "
                                + "peer_raw_score, teacher_score, final_score, class_rank, credibility_score, "
                                + "credibility_grade, publish_status, deleted, version, create_by, update_by, "
                                + "create_time, update_time) values (?,?,?,?,?,?,?,0,?,?,'未发布',0,0,?,?,now(),now())",
                        assignmentId, s.get("student_id"), auto, peer, peerRaw,
                        teacher.doubleValue() <= 0 ? null : teacher, fin, cred, grade,
                        UserContext.userId(), UserContext.userId());
            }
            n++;
        }
        // 班级排名
        jdbc.update("update score_summary s join (select id, rank() over (order by final_score desc) rk "
                + "from score_summary where assignment_id = ? and deleted = 0) t on t.id = s.id "
                + "set s.class_rank = t.rk", assignmentId);
        biz.log("成绩审核", "合成", String.valueOf(assignmentId), "合成成绩 " + n + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", n);
        return R.ok(out);
    }

    /** 人工改分：必须填写依据，原计算结果保留在快照表 */
    @Transactional
    @PostMapping("/score/{id}/adjust")
    public R<Void> scoreAdjust(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Object fs = b.get("finalScore");
        if (fs == null) {
            throw new BizException("请填写调整后的分数");
        }
        double score = ((Number) fs).doubleValue();
        if (score < 0 || score > 100) {
            throw new BizException("分数需在 0-100 之间");
        }
        String reason = str(b.get("adjustReason"));
        if (reason == null || reason.length() < 10) {
            throw new BizException("人工调整必须填写不少于 10 字的依据");
        }
        jdbc.update("update score_summary set final_score = ?, adjust_reason = ?, update_time = now() where id = ?",
                score, reason, id);
        biz.log("成绩审核", "改分", String.valueOf(id), "改分为 " + score + "，依据：" + reason);
        return R.ok();
    }

    /** 成绩发布：全部互评完成或标记超时后才能发布 */
    @Transactional
    @PostMapping("/score/publish")
    public R<Map<String, Object>> scorePublish(@RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        Long assignmentId = ((Number) b.get("assignmentId")).longValue();
        long pending = biz.one("select count(*) from peer_review_task where assignment_id = ? "
                + "and status = '待评' and deleted = 0", assignmentId);
        if (pending > 0) {
            throw new BizException("还有 " + pending + " 个互评任务未完成，不能发布成绩");
        }
        int n = jdbc.update("update score_summary set publish_status = '已发布', publish_time = now(), "
                + "update_time = now() where assignment_id = ? and publish_status = '未发布'", assignmentId);
        biz.pushToCourseStudents(assignmentId, "成绩发布", "作业成绩已发布，可查看各项得分与互评明细");
        biz.log("成绩审核", "发布", String.valueOf(assignmentId), "发布成绩 " + n + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", n);
        return R.ok(out);
    }

    // ============================================================ 申诉
    @GetMapping("/appeal/page")
    public R<Map<String, Object>> appealPage(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String appealType) {
        List<Object> args = new ArrayList<>();
        // join 顺序必须 score_summary → code_assignment → course，course 依赖 a.id，放前面会 Unknown column
        StringBuilder where = new StringBuilder(" from appeal_record r left join sys_user u on u.id = r.student_id "
                + "left join score_summary s on s.id = r.score_id left join code_assignment a "
                + "on a.id = s.assignment_id left join course c on c.id = a.course_id "
                + "left join sys_user h on h.id = r.handler_id where r.deleted = 0");
        if (status != null && !status.isBlank()) {
            where.append(" and r.status = ?");
            args.add(status);
        }
        if (appealType != null && !appealType.isBlank()) {
            where.append(" and r.appeal_type = ?");
            args.add(appealType);
        }
        if (UserContext.isStudent()) {
            where.append(" and r.student_id = ?");
            args.add(UserContext.userId());
        }
        String select = "r.id, r.appeal_no, r.score_id, r.student_id, r.appeal_type, r.reason, r.expect_score, "
                + "r.evidence_urls, r.status, r.handler_id, r.handle_result, r.handle_time, r.create_time, "
                + "u.real_name as student_name, u.student_no, u.class_name, u.avatar as student_avatar, "
                + "a.title as assignment_title, c.course_name, s.final_score, s.peer_raw_score, "
                + "s.teacher_score, s.credibility_grade, h.real_name as handler_name";
        return R.ok(biz.paged(select, where.toString(), args, "r.id desc", page, size));
    }

    /** 学生发起申诉 */
    @Transactional
    @PostMapping("/appeal")
    public R<Long> appealCreate(@RequestBody Map<String, Object> b) {
        if (!UserContext.isStudent()) {
            throw new BizException(403, "仅学生可发起申诉");
        }
        Long scoreId = ((Number) b.get("scoreId")).longValue();
        Map<String, Object> s = jdbc.queryForList("select * from score_summary where id = ? and deleted = 0", scoreId)
                .stream().findFirst().orElseThrow(() -> new BizException("成绩记录不存在"));
        if (!UserContext.userId().equals(((Number) s.get("student_id")).longValue())) {
            throw new BizException(403, "只能对本人成绩发起申诉");
        }
        if (!"已发布".equals(String.valueOf(s.get("publish_status")))) {
            throw new BizException("成绩尚未发布，不能申诉");
        }
        // 需求 5.6：发布后 7 天内提交，逾期只能线下联系教学办
        LocalDateTime pubAt = BizService.toTime(s.get("publish_time"));
        if (pubAt != null && pubAt.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BizException("成绩已发布超过 7 天（" + pubAt.toLocalDate() + " 发布），申诉通道已关闭");
        }
        // 需求 5.6：查重误判类申诉必须附证据
        if ("查重误判".equals(str(b.get("appealType")))
                && (str(b.get("evidenceUrls")) == null || str(b.get("evidenceUrls")).isBlank())) {
            throw new BizException("查重误判申诉必须上传证据（代码对比截图或原始工程）");
        }
        String reason = str(b.get("reason"));
        if (reason == null || reason.length() < 20) {
            throw new BizException("申诉理由不少于 20 字");
        }
        long dup = biz.one("select count(*) from appeal_record where score_id = ? and status in ('待受理','处理中')",
                scoreId);
        if (dup > 0) {
            throw new BizException("该成绩已有在处理中的申诉");
        }
        String no = "SS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%04d", biz.one("select count(*) from appeal_record") + 1);
        Long id = biz.insert("insert into appeal_record (appeal_no, score_id, student_id, appeal_type, reason, "
                        + "expect_score, evidence_urls, status, deleted, version, create_by, update_by, "
                        + "create_time, update_time) values (?,?,?,?,?,?,?,'待受理',0,0,?,?,now(),now())",
                no, scoreId, UserContext.userId(), str(b.get("appealType")), reason, b.get("expectScore"),
                str(b.get("evidenceUrls")), UserContext.userId(), UserContext.userId());
        biz.log("申诉处理", "新增", no, "学生发起申诉");
        return R.ok(id);
    }

    /** 处理申诉：处理人不能是原评分教师 */
    @Transactional
    @PostMapping("/appeal/{id}/handle")
    public R<Void> appealHandle(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        biz.requireTeacherOrAbove();
        String status = str(b.get("status"));
        if (!List.of("处理中", "已驳回", "已改分", "需重新互评").contains(status)) {
            throw new BizException("处理状态不合法");
        }
        String result = str(b.get("handleResult"));
        if (result == null || result.length() < 5) {
            throw new BizException("处理结果不少于 5 字");
        }
        Map<String, Object> r = jdbc.queryForList("select * from appeal_record where id = ?", id)
                .stream().findFirst().orElseThrow(() -> new BizException("申诉不存在"));
        Long scoreId = ((Number) r.get("score_id")).longValue();
        Map<String, Object> sc = jdbc.queryForList("select s.*, a.course_id, c.teacher_id from score_summary s "
                        + "join code_assignment a on a.id = s.assignment_id join course c on c.id = a.course_id "
                        + "where s.id = ?", scoreId).stream().findFirst().orElseThrow();
        if (!UserContext.isAdmin() && !UserContext.isAcademic()
                && UserContext.userId().equals(((Number) sc.get("teacher_id")).longValue())) {
            throw new BizException(403, "原评分教师不能受理本人课程的申诉");
        }
        jdbc.update("update appeal_record set status = ?, handler_id = ?, handle_result = ?, handle_time = now(), "
                + "update_time = now() where id = ?", status, UserContext.userId(), result, id);
        if ("已改分".equals(status)) {
            Object ns = b.get("newScore");
            if (ns == null) {
                throw new BizException("改分需填写新分数");
            }
            jdbc.update("update score_summary set final_score = ?, adjust_reason = ?, update_time = now() "
                    + "where id = ?", ns, "申诉改分：" + result, scoreId);
        }
        biz.log("申诉处理", "处理", String.valueOf(r.get("appeal_no")), "申诉处理为 " + status);
        return R.ok();
    }

    // ============================================================ 可信度
    @GetMapping("/credibility/page")
    public R<Map<String, Object>> credibilityPage(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String grade,
                                                  @RequestParam(required = false) Long assignmentId,
                                                  @RequestParam(required = false) String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from credibility_snapshot c left join sys_user u "
                + "on u.id = c.student_id left join code_assignment a on a.id = c.assignment_id where c.deleted = 0");
        if (grade != null && !grade.isBlank()) {
            where.append(" and c.grade = ?");
            args.add(grade);
        }
        if (assignmentId != null) {
            where.append(" and c.assignment_id = ?");
            args.add(assignmentId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (u.real_name like ? or u.student_no like ? or c.snapshot_no like ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
            args.add(kw);
        }
        if (UserContext.isStudent()) {
            where.append(" and c.student_id = ?");
            args.add(UserContext.userId());
        } else if (UserContext.isTeacher()) {
            where.append(" and a.course_id in (select id from course where teacher_id = ?)");
            args.add(UserContext.userId());
        }
        String select = "c.id, c.snapshot_no, c.task_id, c.score_id, c.submission_id, c.student_id, c.assignment_id, "
                + "c.rule_version, c.s_deviation, c.s_comment, c.s_finish_rate, c.s_similarity, c.s_consistency, "
                + "c.total_score, c.grade, c.conclusion, c.manual_flag, c.manual_reason, c.calc_time, "
                + "u.real_name as student_name, u.student_no, a.title as assignment_title";
        return R.ok(biz.paged(select, where.toString(), args, "c.id desc", page, size));
    }

    /** 可信度明细：五因子得分 + 输入快照 + 结论 + 未满足条件 */
    @GetMapping("/credibility/{id}")
    public R<Map<String, Object>> credibilityDetail(@PathVariable Long id) {
        Map<String, Object> c = biz.row(jdbc.queryForList(
                "select c.*, u.real_name as student_name, u.student_no, a.title as assignment_title, "
                        + "t.task_no, s.comment, s.total_score as peer_total, s.low_quality "
                        + "from credibility_snapshot c left join sys_user u on u.id = c.student_id "
                        + "left join code_assignment a on a.id = c.assignment_id "
                        + "left join peer_review_task t on t.id = c.task_id "
                        + "left join peer_review_score s on s.id = c.score_id where c.id = ?", id)
                .stream().findFirst().orElse(null));
        if (c == null) {
            throw new BizException("留痕记录不存在");
        }
        Map<String, Object> rule = biz.currentRule();
        c.put("weights", Map.of(
                "deviation", rule.get("weight_deviation"),
                "comment", rule.get("weight_comment"),
                "finishRate", rule.get("weight_finish_rate"),
                "similarity", rule.get("weight_similarity"),
                "consistency", rule.get("weight_consistency")));
        c.put("thresholds", Map.of(
                "high", rule.get("grade_high"),
                "mid", rule.get("grade_mid"),
                "low", rule.get("grade_low")));
        // 结果解释：主要影响因素（按加权贡献排序）
        List<Map<String, Object>> parts = new ArrayList<>();
        double[] s = {BizService.num(c.get("sDeviation"), 0), BizService.num(c.get("sComment"), 0),
                BizService.num(c.get("sFinishRate"), 0), BizService.num(c.get("sSimilarity"), 0),
                BizService.num(c.get("sConsistency"), 0)};
        String[] names = {"评分偏离度", "评语有效长度", "互评完成率", "提交相似度", "评分一致性"};
        double[] ws = {BizService.num(rule.get("weight_deviation"), 25), BizService.num(rule.get("weight_comment"), 20),
                BizService.num(rule.get("weight_finish_rate"), 20), BizService.num(rule.get("weight_similarity"), 20),
                BizService.num(rule.get("weight_consistency"), 15)};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", names[i]);
            p.put("score", s[i]);
            p.put("weight", ws[i]);
            p.put("contribution", BizService.round1(s[i] * ws[i] / 100.0));
            parts.add(p);
        }
        parts.sort((a, b) -> Double.compare(BizService.num(b.get("contribution"), 0),
                BizService.num(a.get("contribution"), 0)));
        c.put("factors", parts);
        c.put("mainFactor", parts.isEmpty() ? "" : parts.get(0).get("name"));
        return R.ok(c);
    }

    /** 规则列表（含历史版本，历史计算不受新规则影响） */
    @GetMapping("/credibility/rule")
    public R<Map<String, Object>> credibilityRule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("current", biz.row(biz.currentRule()));
        m.put("versions", biz.rows(jdbc.queryForList(
                "select r.*, u.real_name as operator_name from credibility_rule r "
                        + "left join sys_user u on u.id = r.operator_id order by r.effect_time desc")));
        return R.ok(m);
    }

    /** 保存规则：权重合计必须等于 100，生成新版本并让旧版本失效 */
    @Transactional
    @PostMapping("/credibility/rule")
    public R<Map<String, Object>> ruleSave(@RequestBody Map<String, Object> b) {
        biz.requireStaff();
        if (!(UserContext.isAdmin() || UserContext.isAcademic() || UserContext.isTeacher())) {
            throw new BizException(403, "仅教师及以上角色可修改规则");
        }
        double[] w = {BizService.num(b.get("weightDeviation"), 25), BizService.num(b.get("weightComment"), 20),
                BizService.num(b.get("weightFinishRate"), 20), BizService.num(b.get("weightSimilarity"), 20),
                BizService.num(b.get("weightConsistency"), 15)};
        double sum = Arrays.stream(w).sum();
        if (Math.abs(sum - 100) > 0.01) {
            throw new BizException("权重合计必须等于 100%，当前为 " + BizService.round1(sum) + "%");
        }
        String ver = str(b.get("ruleVersion"));
        if (ver == null || ver.isBlank()) {
            throw new BizException("请填写规则版本号");
        }
        if (biz.one("select count(*) from credibility_rule where rule_version = ?", ver) > 0) {
            throw new BizException("版本号 " + ver + " 已存在");
        }
        jdbc.update("update credibility_rule set status = '已失效', update_time = now() where status = '生效中'");
        Long id = biz.insert("insert into credibility_rule (rule_version, rule_name, weight_deviation, "
                        + "weight_comment, weight_finish_rate, weight_similarity, weight_consistency, grade_high, "
                        + "grade_mid, grade_low, similarity_alert, effect_time, operator_id, status, remark, "
                        + "deleted, version, create_by, update_by, create_time, update_time) "
                        + "values (?,?,?,?,?,?,?,?,?,?,?,now(),?,'生效中',?,0,0,?,?,now(),now())",
                ver, str(b.get("ruleName")) == null ? "规则 " + ver : str(b.get("ruleName")),
                w[0], w[1], w[2], w[3], w[4],
                BizService.num(b.get("gradeHigh"), 85), BizService.num(b.get("gradeMid"), 65),
                BizService.num(b.get("gradeLow"), 45), BizService.num(b.get("similarityAlert"), 70),
                UserContext.userId(), str(b.get("remark")),
                UserContext.userId(), UserContext.userId());
        biz.log("规则配置", "新增", ver, "保存可信度规则 " + ver);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("ruleVersion", ver);
        return R.ok(out);
    }

    /** 主动重算：只影响新计算或主动重算的数据 */
    @PostMapping("/credibility/recalc")
    public R<Map<String, Object>> credibilityRecalc(@RequestBody(required = false) Map<String, Object> b) {
        Object aid = b == null ? null : b.get("assignmentId");
        return R.ok(biz.recalc(aid == null ? null : ((Number) aid).longValue()));
    }

    /** 人工修正结论：必须填原因，原始计算结果保留 */
    @Transactional
    @PostMapping("/credibility/{id}/manual")
    public R<Void> manualAdjust(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        if (!(UserContext.isTeacher() || UserContext.isAcademic() || UserContext.isAdmin())) {
            throw new BizException(403, "仅教师及以上角色可人工修正");
        }
        String reason = str(b.get("manualReason"));
        if (reason == null || reason.length() < 5) {
            throw new BizException("人工修正必须填写不少于 5 字的原因");
        }
        String grade = str(b.get("grade"));
        if (grade != null && !List.of("高可信", "中可信", "较低可信", "低可信").contains(grade)) {
            throw new BizException("等级不合法");
        }
        jdbc.update("update credibility_snapshot set manual_flag = 1, manual_reason = ?, "
                + "grade = coalesce(?, grade), update_time = now() where id = ?", reason, grade, id);
        biz.log("可信度", "人工修正", String.valueOf(id), "修正原因：" + reason);
        return R.ok();
    }

    // ============================================================ 统计
    @GetMapping("/stats/summary")
    public R<Map<String, Object>> statsSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("byCourse", biz.rows(jdbc.queryForList(
                "select c.course_name as name, round(avg(s.final_score), 1) as avgScore, count(s.id) as value "
                        + "from score_summary s join code_assignment a on a.id = s.assignment_id "
                        + "join course c on c.id = a.course_id where s.deleted = 0 group by c.course_name")));
        m.put("byClass", biz.rows(jdbc.queryForList(
                "select u.class_name as name, round(avg(s.final_score), 1) as avgScore, count(s.id) as value "
                        + "from score_summary s join sys_user u on u.id = s.student_id "
                        + "where s.deleted = 0 group by u.class_name")));
        m.put("gradeByCourse", biz.rows(jdbc.queryForList(
                "select c.course_name as name, "
                        + "sum(case when s.credibility_grade = '高可信' then 1 else 0 end) as high, "
                        + "sum(case when s.credibility_grade = '中可信' then 1 else 0 end) as mid, "
                        + "sum(case when s.credibility_grade = '较低可信' then 1 else 0 end) as low, "
                        + "sum(case when s.credibility_grade = '低可信' then 1 else 0 end) as veryLow "
                        + "from score_summary s join code_assignment a on a.id = s.assignment_id "
                        + "join course c on c.id = a.course_id where s.deleted = 0 group by c.course_name")));
        m.put("lateByMonth", biz.rows(jdbc.queryForList(
                "select date_format(submit_time, '%Y-%m') as name, "
                        + "sum(case when is_late = 1 then 1 else 0 end) as late, count(*) as value "
                        + "from code_submission where deleted = 0 group by 1 order by 1")));
        m.put("passByAssignment", biz.rows(jdbc.queryForList(
                "select a.title as name, round(avg(case when s.total_count = 0 then 0 "
                        + "else s.pass_count * 100.0 / s.total_count end), 1) as value "
                        + "from code_submission s join code_assignment a on a.id = s.assignment_id "
                        + "where s.compile_result = '通过' and s.deleted = 0 group by a.id, a.title "
                        + "order by a.id limit 10")));
        m.put("credibilityTrend", biz.rows(jdbc.queryForList(
                "select date_format(calc_time, '%Y-%m') as name, round(avg(total_score), 1) as value "
                        + "from credibility_snapshot where deleted = 0 group by 1 order by 1")));
        m.put("appealByType", biz.rows(jdbc.queryForList(
                "select appeal_type as name, count(*) as value from appeal_record where deleted = 0 group by 1")));
        return R.ok(m);
    }

    // ============================================================ 学生门户
    @GetMapping("/portal/home")
    public R<Map<String, Object>> portalHome() {
        Map<String, Object> m = new LinkedHashMap<>();
        Long uid = UserContext.userId();
        m.put("student", biz.row(jdbc.queryForList(
                "select u.id, u.real_name, u.student_no, u.class_name, u.avatar, u.credit_score, "
                        + "o.org_name from sys_user u left join sys_org o on o.id = u.dept_id where u.id = ?", uid)
                .stream().findFirst().orElse(null)));
        m.put("todoAssignments", biz.rows(jdbc.queryForList(
                "select a.id, a.title, a.deadline, a.status, a.language_type, c.course_name, c.cover_image, "
                        + "c.teacher_id, (select count(*) from code_submission s where s.assignment_id = a.id "
                        + "and s.student_id = ? and s.deleted = 0) as submitted "
                        + "from code_assignment a join course c on c.id = a.course_id "
                        + "where a.status in ('已发布','提交中') and a.deadline > now() - interval 7 day "
                        + "and c.class_name = (select class_name from sys_user where id = ?) "
                        + "order by a.deadline limit 6", uid, uid)));
        m.put("myPeerTasks", biz.rows(jdbc.queryForList(
                "select t.id, t.task_no, t.deadline, t.status, a.title as assignment_title, a.language_type "
                        + "from peer_review_task t join code_assignment a on a.id = t.assignment_id "
                        + "where t.reviewer_id = ? and t.status = '待评' and t.deleted = 0 order by t.deadline limit 5",
                uid)));
        m.put("myScores", biz.rows(jdbc.queryForList(
                "select s.id, s.final_score, s.credibility_grade, s.publish_status, s.class_rank, "
                        + "a.title as assignment_title, c.course_name from score_summary s "
                        + "join code_assignment a on a.id = s.assignment_id join course c on c.id = a.course_id "
                        + "where s.student_id = ? and s.publish_status = '已发布' order by s.id desc limit 6", uid)));
        m.put("notices", biz.rows(jdbc.queryForList(
                "select id, title, notice_type, publish_time from sys_notice where status = 1 "
                        + "order by publish_time desc limit 5")));
        m.put("myCourses", biz.rows(jdbc.queryForList(
                "select c.id, c.course_name, c.cover_image, c.language_type, c.semester, t.real_name as teacher_name "
                        + "from course c left join sys_user t on t.id = c.teacher_id "
                        + "where c.class_name = (select class_name from sys_user where id = ?) and c.deleted = 0 "
                        + "order by c.id", uid)));
        return R.ok(m);
    }
}
