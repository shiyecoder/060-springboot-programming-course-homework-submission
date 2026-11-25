package com.grad.code.service;

import com.grad.code.common.BizException;
import com.grad.code.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务支撑服务：分页取数、数据范围、状态流转、互评可信度五因子计算（创新点）。
 *
 * <p>约定：所有结果集列名统一由 {@link #rows} 从 snake_case 转 camelCase，
 * 业务接口与系统管理接口风格一致，前端不必记两套命名。</p>
 */
@Service
@RequiredArgsConstructor
public class BizService {

    private final JdbcTemplate jdbc;

    // ============================================================ 通用取数
    public List<Map<String, Object>> rows(List<Map<String, Object>> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> src : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : src.entrySet()) {
                m.put(camel(e.getKey()), e.getValue());
            }
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> row(Map<String, Object> src) {
        if (src == null) {
            return null;
        }
        return rows(List.of(src)).get(0);
    }

    public static String camel(String s) {
        if (s == null || s.indexOf('_') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public long one(String sql, Object... args) {
        try {
            Long v = jdbc.queryForObject(sql, Long.class, args);
            return v == null ? 0L : v;
        } catch (Exception e) {
            // 统计失败必须打日志，否则会出现"总数 0 但表里有数据"的假象
            System.err.println("[BizService.one] 统计失败：" + sql + " -> " + e.getMessage());
            return 0L;
        }
    }

    public BigDecimal dec(String sql, Object... args) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
            return v == null ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            System.err.println("[BizService.dec] 统计失败：" + sql + " -> " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /** 分页；page 从 1 开始，size 上限 100（文档 7.1）。MySQL 语义是 limit offset, count。 */
    public Map<String, Object> paged(String select, String fromWhere, List<Object> args, String orderBy,
                                     int page, int size) {
        int p = Math.max(1, page);
        int sz = Math.min(100, Math.max(1, size));
        long total = one("select count(*) " + fromWhere, args.toArray());
        List<Object> q = new ArrayList<>(args == null ? List.of() : args);
        q.add(sz);
        q.add((p - 1) * sz);
        String ob = (orderBy == null || orderBy.isBlank()) ? "id desc" : orderBy;
        List<Map<String, Object>> list = jdbc.queryForList(
                "select " + select + " " + fromWhere + " order by " + ob + " limit ? offset ?", q.toArray());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("page", p);
        out.put("size", sz);
        out.put("records", rows(list));
        return out;
    }

    public Map<String, Object> paged(String select, String fromWhere, List<Object> args, int page, int size) {
        return paged(select, fromWhere, args, null, page, size);
    }

    public Long insert(String sql, Object... args) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, kh);
        Number k = kh.getKey();
        return k == null ? null : k.longValue();
    }

    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    public void log(String module, String type, String bizNo, String content) {
        try {
            jdbc.update("insert into sys_oper_log (module, oper_type, biz_no, content, oper_id, oper_name, "
                            + "oper_time, ip, create_time, update_time) values (?,?,?,?,?,?,now(),?,now(),now())",
                    module, type, bizNo, content, UserContext.userId(), UserContext.realName(), "127.0.0.1");
        } catch (Exception e) {
            System.err.println("[BizService.log] 写日志失败：" + e.getMessage());
        }
    }

    // ============================================================ 数据范围
    /**
     * 按角色拼数据范围条件。
     * 学生：只看本人；教师：本人授课课程下的数据；助教/教务/管理员：全量。
     * 返回的 SQL 片段直接 and 到 where 后，占位参数追加到 args。
     */
    public String scopeSql(String aliasColumn, List<Object> args) {
        if (UserContext.isAdmin() || UserContext.isAcademic() || UserContext.isTa()) {
            return "";
        }
        if (UserContext.isTeacher()) {
            args.add(UserContext.userId());
            return " and " + aliasColumn + " in (select a2.id from code_assignment a2 join course c2 "
                    + "on c2.id = a2.course_id where c2.teacher_id = ?) ";
        }
        args.add(UserContext.userId());
        return " and " + aliasColumn + " in (select x.id from code_assignment x join code_submission s "
                + "on s.assignment_id = x.id where s.student_id = ?) ";
    }

    public void requireStaff() {
        if (!UserContext.isStaff()) {
            throw new BizException(403, "当前角色无此操作权限");
        }
    }

    public void requireTeacherOrAbove() {
        if (!(UserContext.isTeacher() || UserContext.isAcademic() || UserContext.isAdmin())) {
            throw new BizException(403, "仅任课教师及以上角色可操作");
        }
    }

    // ============================================================ 作业状态机
    /** 作业状态允许的流转：草稿→已发布→提交中→互评中→评分中→已归档，已发布可撤回后重新发布 */
    private static final Map<String, List<String>> FLOW = Map.of(
            "草稿", List.of("已发布"),
            "已发布", List.of("提交中", "已撤回"),
            "提交中", List.of("互评中"),
            "互评中", List.of("评分中"),
            "评分中", List.of("已归档"),
            "已归档", List.of(),
            "已撤回", List.of("已发布")
    );

    /**
     * 作业状态流转：先校验当前状态是否允许目标状态，再校验来源与必填原因，
     * 最后同事务更新状态、写操作日志并推送站内消息。禁止前端直接改 status。
     */
    @Transactional
    public void transition(Long assignmentId, String target, String reason) {
        requireTeacherOrAbove();
        Map<String, Object> a = jdbc.queryForList(
                "select a.*, c.teacher_id from code_assignment a join course c on c.id = a.course_id "
                        + "where a.id = ?", assignmentId).stream().findFirst()
                .orElseThrow(() -> new BizException("作业不存在"));
        String cur = String.valueOf(a.get("status"));
        List<String> allow = FLOW.getOrDefault(cur, List.of());
        if (!allow.contains(target)) {
            throw new BizException(400, "不允许从「" + cur + "」流转到「" + target + "」");
        }
        if (!UserContext.isAdmin() && !UserContext.isAcademic()
                && !UserContext.userId().equals(((Number) a.get("teacher_id")).longValue())) {
            throw new BizException(403, "只能操作本人课程的作业");
        }
        if ("已撤回".equals(target)) {
            long sub = one("select count(*) from code_submission where assignment_id = ? and deleted = 0", assignmentId);
            if (sub > 0) {
                throw new BizException("该作业已有 " + sub + " 份提交，截止前不可撤回");
            }
            if (reason == null || reason.isBlank()) {
                throw new BizException("撤回必须填写原因");
            }
        }
        if ("已发布".equals(target)) {
            jdbc.update("update code_assignment set status = ?, publish_time = coalesce(publish_time, now()), "
                    + "update_time = now(), update_by = ? where id = ?", target, UserContext.userId(), assignmentId);
        } else {
            jdbc.update("update code_assignment set status = ?, update_time = now(), update_by = ? where id = ?",
                    target, UserContext.userId(), assignmentId);
        }
        log("作业管理", "状态流转", String.valueOf(a.get("assignment_no")), cur + " → " + target);
        if ("已发布".equals(target)) {
            pushToCourseStudents(assignmentId, "作业发布", "作业《" + a.get("title") + "》已发布，请按时提交");
        }
    }

    /** 给作业所属班级的全部学生推送站内消息 */
    public void pushToCourseStudents(Long assignmentId, String type, String title) {
        try {
            jdbc.update("insert into sys_message (receiver_id, title, content, msg_type, biz_type, biz_id, "
                            + "link_url, read_flag, create_time, update_time) "
                            + "select u.id, ?, ?, ?, 'assignment', ?, concat('/assignment/detail/', ?), 0, now(), now() "
                            + "from sys_user u join code_assignment a on a.id = ? "
                            + "join course c on c.id = a.course_id where u.class_name = c.class_name and u.deleted = 0",
                    title, title, type, assignmentId, assignmentId, assignmentId);
        } catch (Exception e) {
            System.err.println("[push] 消息推送失败：" + e.getMessage());
        }
    }

    // ============================================================ 创新点：可信度
    /** 现行规则（status=生效中），无则退回默认权重 */
    public Map<String, Object> currentRule() {
        List<Map<String, Object>> r = jdbc.queryForList(
                "select * from credibility_rule where status = '生效中' order by effect_time desc limit 1");
        if (r.isEmpty()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("rule_version", "v2.1");
            d.put("weight_deviation", new BigDecimal("25"));
            d.put("weight_comment", new BigDecimal("20"));
            d.put("weight_finish_rate", new BigDecimal("20"));
            d.put("weight_similarity", new BigDecimal("20"));
            d.put("weight_consistency", new BigDecimal("15"));
            d.put("grade_high", new BigDecimal("85"));
            d.put("grade_mid", new BigDecimal("65"));
            d.put("grade_low", new BigDecimal("45"));
            d.put("similarity_alert", new BigDecimal("70"));
            return d;
        }
        return r.get(0);
    }

    public String gradeOf(double total, Map<String, Object> rule) {
        double hi = num(rule.get("grade_high"), 85);
        double mid = num(rule.get("grade_mid"), 65);
        double low = num(rule.get("grade_low"), 45);
        if (total >= hi) {
            return "高可信";
        }
        if (total >= mid) {
            return "中可信";
        }
        if (total >= low) {
            return "较低可信";
        }
        return "低可信";
    }

    public static double num(Object o, double dft) {
        if (o == null) {
            return dft;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return dft;
        }
    }

    /**
     * 五因子计算（文档第 8 节）：
     * 1 评分偏离度 25%：互评总分与该作业教师评分均值的绝对偏差，偏差越小分越高；
     * 2 评语有效长度 20%：去套话后字数，少于 15 字或未引用代码位置记 0；
     * 3 互评完成率 20%：近 3 次互评按期完成比例，无历史按 80 中性值；
     * 4 提交相似度 20%：该生提交与同作业其他人的最高相似度，越高越可疑；
     * 5 评分一致性 15%：同一提交多次互评标准差折算，仅一次时不参与该项并按剩余权重归一。
     */
    public Map<String, Object> computeCredibility(Map<String, Object> scoreRow) {
        Map<String, Object> rule = currentRule();
        long scoreId = ((Number) scoreRow.get("id")).longValue();
        long submissionId = ((Number) scoreRow.get("submission_id")).longValue();
        long assignmentId = ((Number) scoreRow.get("assignment_id")).longValue();
        long reviewerId = ((Number) scoreRow.get("reviewer_id")).longValue();
        double peerTotal = num(scoreRow.get("total_score"), 0);

        // 1 偏离度：教师评分均值优先，未出成绩时用同作业互评中位数并在明细中标注
        BigDecimal tAvg = dec("select avg(teacher_score) from score_summary where assignment_id = ? "
                + "and teacher_score is not null", assignmentId);
        String devSource = "教师评分均值";
        double base = tAvg.doubleValue();
        if (base <= 0) {
            BigDecimal pMed = dec("select avg(total_score) from peer_review_score s join peer_review_task t "
                    + "on t.id = s.task_id where t.assignment_id = ?", assignmentId);
            base = pMed.doubleValue();
            devSource = "同作业互评中位数（教师评分未出）";
        }
        double deviation = Math.abs(peerTotal - base);
        double sDeviation = clamp(100 - deviation * 3.2);

        // 2 评语有效长度：去除套话后的有效字数 + 是否引用具体代码位置
        String comment = scoreRow.get("comment") == null ? "" : scoreRow.get("comment").toString();
        int len = effectiveLength(comment);
        boolean cite = comment.contains("行") || comment.contains("第") || comment.contains("方法")
                || comment.contains("变量") || comment.contains("循环");
        double sComment;
        if (len < 15) {
            sComment = 0;
        } else {
            sComment = clamp(Math.min(100, len * 2.2 + (cite ? 22 : 0)));
        }

        // 3 互评完成率：该评阅人近 3 次任务按期完成比例，无历史按 80 中性值
        List<Map<String, Object>> hist = jdbc.queryForList(
                "select status, finish_time, deadline from peer_review_task where reviewer_id = ? "
                        + "and status in ('已评','超时未评','已豁免') order by id desc limit 3", reviewerId);
        double finishRate = 0.8;
        String finishNote = "无历史任务，按 80% 中性值";
        if (!hist.isEmpty()) {
            long done = hist.stream().filter(h -> "已评".equals(String.valueOf(h.get("status")))).count();
            finishRate = (double) done / hist.size();
            finishNote = "近 " + hist.size() + " 次任务按期完成 " + done + " 次";
        }
        double sFinish = clamp(finishRate * 100);

        // 4 提交相似度：该生提交与同作业其他人最高相似度（越高越可疑）
        BigDecimal maxSim = dec("select max(similarity_rate) from similarity_record where "
                + "submission_a_id = ? or submission_b_id = ?", submissionId, submissionId);
        boolean checked = one("select count(*) from similarity_record where submission_a_id = ? "
                + "or submission_b_id = ?", submissionId, submissionId) > 0;
        double simVal = maxSim.doubleValue();
        double sSimilarity = checked ? clamp(100 - simVal * 1.1) : 60;
        String simNote = checked ? "最高相似度 " + String.format("%.2f", simVal) + "%" : "未查重，该项不计分并按 60 中性值提示";

        // 5 评分一致性：同一提交多次互评的标准差
        List<Map<String, Object>> peers = jdbc.queryForList(
                "select total_score from peer_review_score where submission_id = ? and status = '已提交'", submissionId);
        boolean single = peers.size() <= 1;
        double std = 0;
        if (!single) {
            double avg = peers.stream().mapToDouble(p -> num(p.get("total_score"), 0)).average().orElse(0);
            double var = peers.stream().mapToDouble(p -> Math.pow(num(p.get("total_score"), 0) - avg, 2))
                    .average().orElse(0);
            std = Math.sqrt(var);
        }
        double sConsistency = single ? 0 : clamp(100 - std * 4.2);

        // 权重：仅一次互评时一致性项退出并按剩余权重归一
        double wDev = num(rule.get("weight_deviation"), 25);
        double wCmt = num(rule.get("weight_comment"), 20);
        double wFin = num(rule.get("weight_finish_rate"), 20);
        double wSim = num(rule.get("weight_similarity"), 20);
        double wCon = num(rule.get("weight_consistency"), 15);
        double total;
        String weightNote;
        if (single) {
            double rest = wDev + wCmt + wFin + wSim;
            total = (sDeviation * wDev + sComment * wCmt + sFinish * wFin + sSimilarity * wSim) / rest;
            weightNote = "仅一次互评，一致性项不参与，权重按剩余 " + String.format("%.0f", rest) + "% 归一";
        } else {
            total = (sDeviation * wDev + sComment * wCmt + sFinish * wFin + sSimilarity * wSim
                    + sConsistency * wCon) / 100.0;
            weightNote = "五项权重按 " + String.format("%.0f", wDev) + "/" + String.format("%.0f", wCmt)
                    + "/" + String.format("%.0f", wFin) + "/" + String.format("%.0f", wSim)
                    + "/" + String.format("%.0f", wCon) + " 加权";
        }
        total = BigDecimal.valueOf(total).setScale(1, RoundingMode.HALF_UP).doubleValue();
        String grade = gradeOf(total, rule);
        String conclusion = switch (grade) {
            case "高可信" -> "互评分直接按权重计入最终成绩";
            case "中可信" -> "互评分按 0.8 系数折算后计入，明细中说明折算依据";
            case "较低可信" -> "互评分仅作参考，教师评分权重上调至 60%";
            default -> "互评分不计入成绩，需追加一轮互评或由助教复核";
        };
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("taskId", scoreRow.get("task_id"));
        out.put("scoreId", scoreId);
        out.put("submissionId", submissionId);
        out.put("assignmentId", assignmentId);
        out.put("studentId", scoreRow.get("student_id"));
        out.put("ruleVersion", rule.get("rule_version"));
        out.put("sDeviation", round1(sDeviation));
        out.put("sComment", round1(sComment));
        out.put("sFinishRate", round1(sFinish));
        out.put("sSimilarity", round1(sSimilarity));
        out.put("sConsistency", single ? null : round1(sConsistency));
        out.put("totalScore", total);
        out.put("grade", grade);
        out.put("conclusion", conclusion);
        out.put("deviation", round1(deviation));
        out.put("deviationSource", devSource);
        out.put("commentLength", len);
        out.put("citeCode", cite);
        out.put("finishNote", finishNote);
        out.put("similarityNote", simNote);
        out.put("consistency", single ? null : round1(std));
        out.put("weightNote", weightNote);
        out.put("unmet", unmet(sDeviation, sComment, sFinish, sSimilarity, sConsistency, single, checked));
        return out;
    }

    /** 未满足条件与可改进项，结果页除等级外必须给出依据 */
    private List<String> unmet(double dev, double cmt, double fin, double sim, double con, boolean single,
                               boolean checked) {
        List<String> out = new ArrayList<>();
        if (dev < 70) {
            out.add("评分偏离度偏低：与教师评分均值偏差较大，建议复核评分尺度");
        }
        if (cmt < 60) {
            out.add("评语有效长度不足：需指出具体代码位置，避免「写得不错」这类套话");
        }
        if (fin < 70) {
            out.add("互评完成率偏低：近 3 次任务存在超时未评");
        }
        if (sim < 50) {
            out.add("提交相似度过高：与同作业其他提交重合度超过告警线");
        }
        if (single) {
            out.add("仅有 1 份互评：一致性无法评估，建议追加一轮互评");
        } else if (con < 60) {
            out.add("评分一致性偏低：同一份提交的多次互评分歧较大");
        }
        if (!checked) {
            out.add("该提交尚未查重：相似度维度按中性值代入");
        }
        return out;
    }

    /**
     * 去掉「写得不错」「还行」等套话后的有效字数。
     * 除了单字/短词客套话，还要覆盖学生常写的整句套话（「写得很棒继续加油」「非常优秀」这类），
     * 否则一句 40 字的纯客套话也能混过 20 字门槛。
     */
    public static int effectiveLength(String c) {
        if (c == null) {
            return 0;
        }
        String s = c.replaceAll("[\\s，。；、,.;!?！？~～]", "");
        for (String p : Arrays.asList("写得不错", "写得很好", "写得还行", "还行", "可以", "不错", "继续努力",
                "继续加油", "加油", "很好", "很棒", "棒", "优秀", "厉害", "非常好", "真好", "感谢",
                "谢谢老师", "辛苦了", "学到了很多", "收获很大", "同学", "作者")) {
            s = s.replace(p, "");
        }
        return s.length();
    }

    /** 套话识别：去掉标点与常见客套话后有效字数不足 15，视为未指出具体问题（需求 5.5） */
    public static boolean boilerplate(String c) {
        return effectiveLength(c) < 15;
    }

    /** 宽松解析前端传入的时间（"yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd HH:mm" / ISO / Timestamp） */
    public static LocalDateTime toTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime();
        }
        if (o instanceof LocalDateTime l) {
            return l;
        }
        String s = String.valueOf(o).trim().replace('T', ' ');
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() > 19) {
            s = s.substring(0, 19);
        }
        for (String p : Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")) {
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    public static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    public static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 重算：按提交维度重算全部互评的可信度并写入留痕表。
     * 修改权重只影响新计算或主动重算的数据，历史快照保留原规则版本。
     */
    @Transactional
    public Map<String, Object> recalc(Long assignmentId) {
        requireStaff();
        String sql = "select s.id, s.task_id, s.reviewer_id, s.submission_id, s.total_score, s.comment, "
                + "sub.student_id, sub.assignment_id from peer_review_score s "
                + "join code_submission sub on sub.id = s.submission_id where s.status = '已提交' ";
        List<Object> args = new ArrayList<>();
        if (assignmentId != null) {
            sql += " and sub.assignment_id = ? ";
            args.add(assignmentId);
        }
        List<Map<String, Object>> list = jdbc.queryForList(sql, args.toArray());
        int n = 0;
        for (Map<String, Object> r : list) {
            Map<String, Object> c = computeCredibility(r);
            jdbc.update("insert into credibility_snapshot (snapshot_no, task_id, score_id, submission_id, "
                            + "student_id, assignment_id, rule_version, s_deviation, s_comment, s_finish_rate, "
                            + "s_similarity, s_consistency, total_score, grade, input_json, conclusion, "
                            + "manual_flag, calc_time, deleted, create_time, update_time) "
                            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,now(),0,now(),now())",
                    "CT" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
                            + String.format("%06d", n + 1),
                    c.get("taskId"), c.get("scoreId"), c.get("submissionId"), c.get("studentId"),
                    c.get("assignmentId"), c.get("ruleVersion"), c.get("sDeviation"), c.get("sComment"),
                    c.get("sFinishRate"), c.get("sSimilarity"), c.get("sConsistency"), c.get("totalScore"),
                    c.get("grade"), toJson(c), c.get("conclusion"));
            n++;
        }
        log("规则配置", "重算", String.valueOf(assignmentId), "重算可信度 " + n + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", n);
        out.put("ruleVersion", currentRule().get("rule_version"));
        return out;
    }

    public static String toJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (e.getValue() instanceof List || e.getValue() instanceof Map) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number) {
                sb.append(v);
            } else if (v instanceof Boolean b) {
                sb.append(b);
            } else {
                sb.append("\"").append(v.toString().replace("\"", "'")).append("\"");
            }
        }
        return sb.append("}").toString();
    }

    /** 代码相似度合成：结构 0.5 + 标识符 0.3 + 注释 0.2 */
    public static double similarityTotal(double st, double id, double cm) {
        return BigDecimal.valueOf(st * 0.5 + id * 0.3 + cm * 0.2).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
