#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面 01-10：登录 / 工作台 / 课程 / 发布作业 / 作业台账 / 作业详情 / 提交 / 我的提交 / 结果登记 / 批量导入"""
from base import *

# ============================================================ 01 登录
def p01():
    chain = [('01', '发布作业', '题目 · 用例 · 互评份数', '教师'),
             ('02', '代码提交', '本地校验 · 迟交窗口', '学生'),
             ('03', '结果登记', '编译结果 · 用例通过数', '教师/助教'),
             ('04', '互评分配', '轮转算法 · 禁止自评', '系统'),
             ('05', '查重复核', '指纹命中 · 人工确认', '助教'),
             ('06', '成绩合成', '权重校验 · 申诉留痕', '教师/教务')]
    rows = []
    for no, name, desc, who in chain:
        rows.append('<div class="row2"><span class="dot">' + no + '</span><span style="width:78px">' + name
                    + '</span><span class="bar2"></span><span class="lbl">' + desc + '</span></div>')
    deco = ('public class ArrayTask {\\n'
            '  static int sumOf(int[] a) {\\n'
            '    int s = 0;\\n'
            '    for (int v : a) s += v;\\n'
            '    return s;\\n'
            '  }\\n'
            '}')
    body = ('<div class="login-wrap">'
            '<div class="login-left">'
            '<div class="logo"><span class="mk">&lt;/&gt;</span><span>' + TITLE + '</span></div>'
            '<h2>作业从提交到成绩，<br>每一步都有据可查</h2>'
            '<p>课程作业不再靠邮箱收集。代码提交、用例结果、互评分配、相似度核查与成绩申诉在同一处闭环，'
            '互评是否可信由五项因素算出，教师只需复核有疑点的那几条。</p>'
            '<div class="chain">' + ''.join(rows) + '</div>'
            '<div class="stats"><div><b>128</b><span>在授课程</span></div>'
            '<div><b>3 641</b><span>本学期提交</span></div>'
            '<div><b>92.7%</b><span>互评完成率</span></div>'
            '<div><b>0.8s</b><span>相似度比对</span></div></div></div>'
            '<div class="login-right"><div class="login-form">'
            '<h1>登录</h1><div class="tip">使用教务统一账号，学生用学号登录</div>'
            '<div class="fi"><label>账号 / 学号</label><input value="teacher01" readonly></div>'
            '<div class="fi"><label>密码</label><input type="password" value="123456" readonly></div>'
            '<div class="fi" style="display:flex;justify-content:space-between;align-items:center;font-size:13px;color:#5B6770">'
            '<span><input type="checkbox" checked style="height:auto;vertical-align:-2px"> 记住账号</span>'
            '<a href="#">忘记密码</a></div>'
            '<button class="btn primary">登 录</button>'
            '<div class="demo">演示账号（密码均为 <b>123456</b>）<br>'
            '教师 <b>teacher01</b> · 助教 <b>ta01</b> · 学生 <b>20230114</b><br>'
            '教务 <b>academic01</b> · 管理员 <b>admin</b></div>'
            '</div></div>'
            '<div class="deco-code">' + deco + '</div></div>')
    return rawpage('登录', body)


# ============================================================ 02 教师工作台
def p02():
    body = [page_head('教师工作台',
                      '2026 学年第一学期 · Java程序设计（软工2301） · 数据截至 11-02 06:30',
                      '<button class="btn">切换学期</button><button class="btn ghost">导出周报</button>'
                      '<button class="btn primary">' + ic('i-plus', 14) + ' 发布作业</button>')]
    body.append(kpis([
        ('本周待批作业', '4', '份', '2 份今日截止', 'down', 'gold'),
        ('代码提交率', '88.6', '%', '↑ 3.2% 环比', '', ''),
        ('迟交提交', '17', '份', '占提交 4.1%', 'down', ''),
        ('用例平均通过率', '76.3', '%', '↑ 1.8%', '', ''),
        ('互评完成率', '82.4', '%', '6 人未评', 'down', ''),
        ('低质量评语', '9', '条', '已提示 5 条', 'down', ''),
        ('相似度告警', '12', '条', '4 条待复核', 'down', 'gold'),
        ('待处理申诉', '3', '条', '最长 2 天', 'down', ''),
    ], 'cols8'))

    dash = ['<div class="dash">']
    # 1 提交率/迟交率双柱
    dash.append(card('各作业提交率与迟交率', chart_group2(
        ['作业1', '作业2', '作业3', '作业4', '作业5', '作业6'],
        [96, 92, 88, 84, 79, 71], [2, 4, 5, 6, 7, 9]), '6 次作业', 'span2'))
    # 2 用例通过数分布
    dash.append(card('用例通过数分布', chart_vbars(
        [('0/6', 7), ('1/6', 5), ('2/6', 9), ('3/6', 14), ('4/6', 22), ('5/6', 31), ('6/6', 18)], 5), '按提交计 106'))
    # 3 成绩分段环形
    dash.append(card('成绩分段占比', chart_donut(
        [('90-100', 18, '#1E4B3F'), ('80-89', 34, '#3D7A66'), ('70-79', 26, '#C2703A'),
         ('60-69', 15, '#A9B7B0'), ('<60', 7, '#B02A37')], '78.4', '平均分', 120)))
    # 4 散点
    dash.append(card('互评总分与教师评分', chart_scatter(
        [('', 88, 91), ('', 82, 79), ('', 76, 74), ('', 91, 88), ('', 64, 71), ('', 70, 62),
         ('', 85, 86), ('', 58, 66), ('', 93, 90), ('', 79, 76), ('', 68, 70), ('', 87, 82)], 160, 264),
        'n=106 相关系数 0.83'))
    # 5 相似度区间
    dash.append(card('相似度区间分布', chart_hbars(
        [('85-100', 3, '3 条'), ('70-85', 9, '9 条'), ('55-70', 14, '14 条'),
         ('40-55', 27, '27 条'), ('<40', 53, '53 条')], 62), '共 106 对'))
    # 6 可信度等级堆叠
    dash.append(card('互评可信度等级占比（按作业）', chart_stack([
        ('作业1', [('高', 62, '#1E4B3F'), ('中', 28, '#3D7A66'), ('较低', 8, '#C2703A'), ('低', 2, '#B02A37')], '高 62%'),
        ('作业2', [('高', 58, '#1E4B3F'), ('中', 30, '#3D7A66'), ('较低', 9, '#C2703A'), ('低', 3, '#B02A37')], '高 58%'),
        ('作业3', [('高', 47, '#1E4B3F'), ('中', 33, '#3D7A66'), ('较低', 14, '#C2703A'), ('低', 6, '#B02A37')], '高 47%'),
        ('作业4', [('高', 39, '#1E4B3F'), ('中', 35, '#3D7A66'), ('较低', 18, '#C2703A'), ('低', 8, '#B02A37')], '高 39%'),
    ]), '4 个作业已互评', 'span2'))
    dash.append('</div>')
    body.append(''.join(dash))
    return shell('teacher', '教师工作台', '概览 / <b>教师工作台</b>', ''.join(body),
                 '.dash{grid-auto-rows:305px}')


# ============================================================ 03 课程管理
def p03():
    rows = [
        ('<span class="mono">KC2610001</span>', 'Java程序设计', '2026学年第一学期', '计算机学院', '软工2301', 'Java', '42', '6', stag('进行中')),
        ('<span class="mono">KC2610002</span>', 'Java程序设计', '2026学年第一学期', '计算机学院', '软工2302', 'Java', '40', '6', stag('进行中')),
        ('<span class="mono">KC2610003</span>', '数据结构与算法', '2026学年第一学期', '计算机学院', '软工2202', 'Python', '38', '8', stag('进行中')),
        ('<span class="mono">KC2610004</span>', 'Web前端开发', '2026学年第一学期', '计算机学院', '计科2302', 'JavaScript', '45', '5', stag('进行中')),
        ('<span class="mono">KC2610005</span>', 'C语言程序设计', '2026学年第一学期', '计算机学院', '计科2401', 'C', '51', '7', stag('进行中')),
        ('<span class="mono">KC2610006</span>', 'Go语言与云原生', '2026学年第一学期', '软件学院', '软工2101', 'Go', '33', '4', stag('已结课')),
        ('<span class="mono">KC2609007</span>', '面向对象程序设计', '2025学年第二学期', '计算机学院', '软工2201', 'Java', '44', '9', stag('已归档')),
    ]
    body = [page_head('课程管理', '同学期同课程可开多个教学班，学生人数由选课名单汇总，页面只读',
                      '<button class="btn ghost">' + ic('i-export', 14) + ' 导出</button>'
                      '<button class="btn primary">' + ic('i-plus', 14) + ' 新增课程</button>')]
    body.append(toolbar(['input', 'sel:全部学期', 'sel:全部语言', 'sel:全部状态'],
                        '<button class="btn sm">批量结课</button>'))
    body.append(table(['课程编号', '课程名称', '学期', '开课学院', '教学班', '语言', '学生数', '作业数', '状态', '操作'],
                      [r + ('<a href="#">查看</a> · <a href="04-assignment-new.html">发布作业</a>',) for r in rows]))
    body.append(pager(7))
    return shell('teacher', '课程管理', '教学与作业 / <b>课程管理</b>', ''.join(body))


# ============================================================ 04 发布作业
def p04():
    form = [
        ('所属课程', '<select style="width:100%"><option>KC2610001 · Java程序设计 · 软工2301</option>'
                 '<option>KC2610002 · Java程序设计 · 软工2302</option></select>'),
        ('作业标题', '<input value="作业4：文件IO与异常处理" style="width:100%">'),
        ('编程语言', '<select style="width:100%"><option>Java</option><option>Python</option></select>'),
        ('截止时间', '<input value="2026-11-09 23:59:59" style="width:100%">'),
        ('迟交截止', '<input value="2026-11-12 23:59:59" style="width:100%">'),
        ('用例数量', '<input value="6" style="width:100%">'),
        ('互评份数', '<select style="width:100%"><option>3 份</option><option>2 份</option><option>4 份</option></select>'),
        ('成绩权重', '<input value="15.00" style="width:100%">'),
    ]
    fh = ['<div class="fgrid">']
    for k, v in form:
        fh.append('<div class="it"><span>' + k + '</span>' + v + '</div>')
    fh.append('<div class="it full"><span>题目描述</span><textarea style="height:132px">'
              '实现一个日志文件分析工具：读取 logs/app.log，按级别（INFO/WARN/ERROR）统计条数，'
              '输出统计表并按时间倒序打印最近 20 条 ERROR 记录。\n\n'
              '输入：日志文件路径（命令行参数）。\n输出：三行统计 + 最多 20 行 ERROR 明细。\n'
              '要求：文件不存在时抛出受检异常并给出中文提示；使用 try-with-resources 关闭流。</textarea></div>')
    fh.append('</div>')
    form_card = '<div class="card" style="flex:1;min-width:0">' + ''.join(fh) + '</div>'

    right = ['<div style="width:330px;flex:0 0 330px;display:flex;flex-direction:column;gap:12px">']
    right.append('<div class="card"><h2>发布前校验</h2>'
                 + note('<b style="color:#2B7955">✓</b> 截止时间晚于当前时间（11-02 06:30）<br>'
                        '<b style="color:#2B7955">✓</b> 同课程同周次标题无重复<br>'
                        '<b style="color:#2B7955">✓</b> 用例数量 6 在 1–30 之间<br>'
                        '<b style="color:#C2703A">!</b> 迟交窗口 3 天，超过 7 天上限将被拒绝', 'ok')
                 + '</div>')
    right.append('<div class="card"><h2>互评与成绩口径</h2>'
                 '<div class="kv" style="grid-template-columns:78px 1fr">'
                 '<div style="color:#5B6770;text-align:right">互评份数</div><div>3 份 / 人，轮转分配</div>'
                 '<div style="color:#5B6770;text-align:right">互评截止</div><div>截止后 5 天</div>'
                 '<div style="color:#5B6770;text-align:right">成绩构成</div><div>运行结果 40% + 互评 25% + 教师 35%</div>'
                 '<div style="color:#5B6770;text-align:right">可信度门槛</div><div>低于 45 分不计入成绩</div>'
                 '</div></div>')
    right.append('<div class="card"><h2>状态流转</h2>' + flow('草稿') + '</div>')
    right.append('</div>')
    body = [page_head('发布作业', '发布后题目描述不可修改，如需更正只能撤回重发',
                      '<button class="btn">存为草稿</button><button class="btn primary">发布并生成提交入口</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">' + form_card + ''.join(right) + '</div>')
    return shell('teacher', '发布作业', '教学与作业 / <b>发布作业</b>', ''.join(body))


# ============================================================ 05 作业台账
def p05():
    rows = [
        ('<span class="mono">ZY261026001</span>', '作业4：文件IO与异常处理', 'Java程序设计<br><span style="color:#5B6770;font-size:12px">软工2301</span>',
         'Java', '2026-11-09 23:59', '6', '3', '84/86', '97.7%', stag('提交中')),
        ('<span class="mono">ZY261019002</span>', '作业3：多线程售票模拟', 'Java程序设计<br><span style="color:#5B6770;font-size:12px">软工2301</span>',
         'Java', '2026-11-02 23:59', '5', '3', '81/86', '94.2%', stag('互评中')),
        ('<span class="mono">ZY261012003</span>', '作业2：集合与泛型', 'Java程序设计<br><span style="color:#5B6770;font-size:12px">软工2301</span>',
         'Java', '2026-10-26 23:59', '6', '3', '86/86', '100%', stag('评分中')),
        ('<span class="mono">ZY261005004</span>', '作业1：数组与字符串处理', 'Java程序设计<br><span style="color:#5B6770;font-size:12px">软工2301</span>',
         'Java', '2026-10-19 23:59', '4', '2', '86/86', '100%', stag('已归档')),
        ('<span class="mono">ZY261026005</span>', '实验3：二叉树遍历', '数据结构与算法<br><span style="color:#5B6770;font-size:12px">软工2202</span>',
         'Python', '2026-11-06 23:59', '8', '3', '33/38', '86.8%', stag('提交中')),
        ('<span class="mono">ZY261019006</span>', '实验2：链表与栈', '数据结构与算法<br><span style="color:#5B6770;font-size:12px">软工2202</span>',
         'Python', '2026-10-29 23:59', '6', '3', '37/38', '97.4%', stag('互评中')),
        ('<span class="mono">ZY261028007</span>', '大作业：响应式企业站', 'Web前端开发<br><span style="color:#5B6770;font-size:12px">计科2302</span>',
         'JavaScript', '2026-11-20 23:59', '10', '4', '12/45', '26.7%', stag('已发布')),
        ('<span class="mono">ZY261025008</span>', '作业5：指针与结构体', 'C语言程序设计<br><span style="color:#5B6770;font-size:12px">计科2401</span>',
         'C', '2026-11-04 23:59', '7', '3', '44/51', '86.3%', stag('草稿')),
    ]
    body = [page_head('作业台账', '共 8 个作业：草稿 1 · 已发布 2 · 提交中 2 · 互评中 2 · 评分中 1',
                      '<button class="btn ghost">' + ic('i-export', 14) + ' 导出</button>'
                      '<button class="btn primary">' + ic('i-plus', 14) + ' 发布作业</button>')]
    body.append('<div class="tabs"><a class="on" href="#">全部<span class="c">8</span></a>'
                '<a href="#">草稿<span class="c">1</span></a><a href="#">已发布<span class="c">2</span></a>'
                '<a href="#">提交中<span class="c">2</span></a><a href="#">互评中<span class="c">2</span></a>'
                '<a href="#">评分中<span class="c">1</span></a><a href="#">已归档<span class="c">1</span></a></div>')
    body.append(toolbar(['input', 'sel:全部课程', 'sel:全部状态', 'sel:全部语言'],
                        '<button class="btn sm">批量催交</button><button class="btn sm">批量催评</button>'))
    body.append(table(['作业编号', '标题', '课程', '语言', '截止时间', '用例', '互评份数', '提交/应交', '提交率', '状态', '操作'],
                      [r + ('<a href="06-assignment-detail.html">详情</a> · <a href="09-result-entry.html">登记</a>',) for r in rows]))
    body.append(pager(8))
    return shell('teacher', '作业台账', '教学与作业 / <b>作业台账</b>', ''.join(body))


# ============================================================ 06 作业详情
def p06():
    desc = ['import java.nio.file.*;', 'import java.util.*;', '',
            'public class LogAnalyzer {',
            '    // 按级别统计并输出最近 20 条 ERROR',
            '    public static void main(String[] args) throws Exception {',
            '        Path p = Paths.get(args[0]);',
            '        if (!Files.exists(p)) {',
            '            throw new IllegalStateException("日志文件不存在：" + p);',
            '        }',
            '        Map<String, Integer> stat = new LinkedHashMap<>();',
            '        try (Stream<String> lines = Files.lines(p)) {',
            '            lines.forEach(l -> stat.merge(levelOf(l), 1, Integer::sum));',
            '        }',
            '        stat.forEach((k, v) -> System.out.println(k + " " + v));',
            '    }',
            '}']
    sub = [
        ('TD2611020031', '11-02 21:14', '林嘉禾', '20230114', '通过', '5/6', stag('已登记')),
        ('TD2611020029', '11-02 20:02', '王雨桐', '20230115', '通过', '6/6', stag('已登记')),
        ('TD2611020027', '11-02 19:38', '周子豪', '20230116', '编译失败', '0/6', stag('已登记')),
        ('TD2611020024', '11-02 18:51', '陈思远', '20230117', '通过', '4/6', stag('待登记')),
    ]
    body = [page_head('作业4：文件IO与异常处理',
                      '<span class="mono" style="font-size:12px">ZY261026001</span> · Java程序设计（软工2301） · 用例 6 · 互评 3 份',
                      '<button class="btn">编辑草稿</button><button class="btn">撤回</button>'
                      '<button class="btn ghost">' + ic('i-shuffle', 14) + ' 分配互评</button>'
                      '<button class="btn primary">' + ic('i-shield', 14) + ' 开始查重</button>')]
    body.append('<div class="card" style="padding:12px 18px;margin-bottom:14px">' + flow('提交中') + '</div>')

    left = ['<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:12px">']
    left.append('<div class="card"><h2>题目描述</h2>'
                '<div style="font-size:13px;line-height:1.9;color:#3A4750">实现一个日志文件分析工具：读取 logs/app.log，'
                '按级别（INFO/WARN/ERROR）统计条数，输出统计表并按时间倒序打印最近 20 条 ERROR 记录。<br><br>'
                '<b>输入</b>：日志文件路径（命令行参数）<br><b>输出</b>：三行统计 + 最多 20 行 ERROR 明细<br>'
                '<b>要求</b>：文件不存在时抛出受检异常并给出中文提示；使用 try-with-resources 关闭流。</div>'
                '<div class="code-head" style="margin-top:14px">' + ic('i-file', 14)
                + '<span class="fnm">LogAnalyzer.java</span><span class="spacer"></span>'
                '<span style="font-size:11px;color:#5B6770">参考答案 24 行</span></div>'
                + code_block(desc, 1, (12, 13), 'h180') + '</div>')
    left.append('<div class="card"><h2>提交进度</h2>'
                + table(['提交编号', '提交时间', '学生', '学号', '编译结果', '用例', '状态'],
                        [(('<span class="mono">' + r[0] + '</span>'), r[1], r[2], ('<span class="mono">' + r[3] + '</span>'),
                          stag(r[4]) if r[4] != '未运行' else stag('未运行'), r[5], r[6]) for r in sub])
                + pager(84) + '</div>')
    left.append('</div>')

    right = ['<div style="width:320px;flex:0 0 320px;display:flex;flex-direction:column;gap:12px">']
    right.append('<div class="summary" style="grid-template-columns:1fr 1fr;margin-bottom:0">'
                 '<div><div class="k">已提交</div><div class="v">84 / 86</div></div>'
                 '<div><div class="k">迟交</div><div class="v">3 份</div></div>'
                 '<div><div class="k">用例均通过</div><div class="v">4.6 / 6</div></div>'
                 '<div><div class="k">剩余</div><div class="v num-gold">6 天 17 时</div></div></div>')
    right.append('<div class="card"><h2>用例与互评配置</h2>'
                 + kvrows([('用例数量', '6 条（上限即登记上限）'), ('互评份数', '3 份 / 人'),
                           ('互评截止', '2026-11-14 23:59'), ('成绩权重', '15.00 / 100')]) + '</div>')
    right.append('<div class="card"><h2>状态流水</h2>' + timeline([
        ('ok', '已发布作业', '陈立群', '2026-10-26 15:40'),
        ('ok', '进入提交中', '系统', '2026-10-26 15:40'),
        ('', '登记 3 条运行结果', '周敏（助教）', '2026-11-02 19:40'),
        ('', '待截止后进入互评', '系统', '2026-11-09 23:59'),
    ]) + '</div>')
    right.append('</div>')
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">' + ''.join(left) + ''.join(right) + '</div>')
    return shell('teacher', '作业详情', '教学与作业 / 作业台账 / <b>作业详情</b>', ''.join(body))


# ============================================================ 07 提交代码
def p07():
    code = ['import java.nio.file.*;', 'import java.util.*;', 'import java.util.stream.*;', '',
            'public class LogAnalyzer {',
            '',
            '    private static String levelOf(String line) {',
            '        if (line.contains("ERROR")) return "ERROR";',
            '        if (line.contains("WARN")) return "WARN";',
            '        return "INFO";',
            '    }',
            '',
            '    public static void main(String[] args) throws Exception {',
            '        Path p = Paths.get(args[0]);',
            '        if (!Files.exists(p)) {',
            '            throw new IllegalStateException("日志文件不存在：" + p);',
            '        }',
            '',
            '        Map<String, Integer> stat = new LinkedHashMap<>();',
            '        List<String> errors = new ArrayList<>();',
            '        try (Stream<String> lines = Files.lines(p)) {',
            '            lines.forEach(l -> {',
            '                stat.merge(levelOf(l), 1, Integer::sum);',
            '                if (levelOf(l).equals("ERROR")) errors.add(l);',
            '            });',
            '        }',
            '',
            '        stat.forEach((k, v) -> System.out.println(k + " " + v));',
            '        errors.stream()',
            '              .sorted(Comparator.reverseOrder())',
            '              .limit(20)',
            '              .forEach(System.out::println);',
            '    }',
            '}']
    body = [page_head('提交作业4：文件IO与异常处理',
                      'Java程序设计（软工2301） · 剩余 6 天 17 小时 · 迟交窗口至 11-12 23:59',
                      '<span class="tag t-amber" style="align-self:center">已提交 2 次 · 以最后一次为准</span>')]
    left = ['<div style="width:44%;min-width:0;flex:0 0 44%;display:flex;flex-direction:column;gap:12px">']
    left.append('<div class="card" style="flex:1"><h2>题目描述</h2>'
                '<div style="font-size:13px;line-height:1.9;color:#3A4750">实现一个日志文件分析工具：读取 logs/app.log，'
                '按级别（INFO/WARN/ERROR）统计条数，输出统计表并按时间倒序打印最近 20 条 ERROR 记录。</div>'
                '<div style="margin:12px 0 6px;font-size:13px;font-weight:500">用例说明（6 条）</div>'
                '<table style="font-size:12px"><thead><tr><th>#</th><th>输入</th><th>期望输出</th><th style="width:56px">分值</th></tr></thead><tbody>'
                '<tr><td>1</td><td class="mono">标准日志 200 行</td><td>三行统计</td><td>15</td></tr>'
                '<tr><td>2</td><td class="mono">文件不存在</td><td>中文异常提示</td><td>15</td></tr>'
                '<tr><td>3</td><td class="mono">空文件</td><td>三行 0</td><td>15</td></tr>'
                '<tr><td>4</td><td class="mono">ERROR &gt; 20 条</td><td>只输出 20 条</td><td>20</td></tr>'
                '<tr><td>5</td><td class="mono">含大文件 12MB</td><td>流式处理不 OOM</td><td>20</td></tr>'
                '<tr><td>6</td><td class="mono">非法编码行</td><td>跳过不中断</td><td>15</td></tr>'
                '</tbody></table>'
                + '<div style="margin-top:12px">' + note('提交前本地校验：扩展名 .java 与所选语言一致 · 单文件 ≤ 200KB · 代码 ≤ 2000 行', 'info') + '</div>'
                '</div>')
    left.append('</div>')

    right = ['<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:10px">']
    right.append('<div class="card" style="flex:1;display:flex;flex-direction:column">'
                 '<div class="code-head">' + ic('i-code', 14)
                 + '<span class="fnm">LogAnalyzer.java</span>'
                 + '<span class="tag t-green">行数 33 / 2000</span><span class="tag t-green">18.4 KB</span>'
                 + '<span class="spacer"></span><button class="btn sm">上传文件</button>'
                 '<button class="btn sm">格式化</button></div>'
                 + code_block(code, 1, (20, 21, 22), 'h420')
                 + '<div style="display:flex;align-items:center;gap:10px;margin-top:10px">'
                 '<span style="font-size:12px;color:#5B6770">第 2 次提交 · 上次 2026-11-02 21:14 · 用例 5/6</span>'
                 '<span style="flex:1"></span><button class="btn">保存草稿</button>'
                 '<button class="btn primary">提交代码</button></div></div>')
    right.append('<div class="card"><h2>提交记录</h2>'
                 + table(['版本', '提交时间', '行数', '编译结果', '用例', '是否迟交', '操作'], [
                     ('v2', '2026-11-02 21:14', '33', stag('通过'), '5/6', stag('按时'), '<a href="#">对比</a> · <a href="#">查看</a>'),
                     ('v1', '2026-11-01 22:40', '29', stag('编译失败'), '0/6', stag('按时'), '<a href="#">对比</a>'),
                 ]) + '</div>')
    right.append('</div>')
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:stretch">' + ''.join(left) + ''.join(right) + '</div>')
    return shell('student', '提交代码', '我的学习 / <b>提交代码</b>', ''.join(body))


# ============================================================ 08 我的提交
def p08():
    rows = [
        ('<span class="mono">TD26110200031</span>', '作业4：文件IO与异常处理', 'Java', 'v2 / 2 次', '2026-11-02 21:14',
         '33', stag('通过'), '5/6', stag('按时'), '待互评', '—'),
        ('<span class="mono">TD26110100012</span>', '作业3：多线程售票模拟', 'Java', 'v1 / 1 次', '2026-11-01 22:40',
         '47', stag('通过'), '5/5', stag('按时'), stag('已评分'), '<b>87.4</b>'),
        ('<span class="mono">TD26102500008</span>', '作业2：集合与泛型', 'Java', 'v3 / 3 次', '2026-10-25 20:11',
         '38', stag('通过'), '6/6', stag('按时'), stag('已评分'), '<b>91.2</b>'),
        ('<span class="mono">TD26101800003</span>', '作业1：数组与字符串处理', 'Java', 'v1 / 1 次', '2026-10-18 23:41',
         '26', stag('通过'), '4/4', stag('迟交'), stag('已评分'), '<b>78.6</b>'),
    ]
    body = [page_head('我的提交', 'Java程序设计（软工2301） · 4 次作业 · 平均 85.7 分',
                      '<button class="btn ghost">' + ic('i-download', 14) + ' 下载我的代码</button>')]
    body.append(kpis([('已提交作业', '4', '/ 4', '', '', ''), ('按时提交', '3', '次', '', '', ''),
                      ('迟交', '1', '次', '', 'down', ''), ('累计提交版本', '7', '个', '', '', ''),
                      ('平均互评得分', '84.6', '分', '', '', ''), ('当前成绩', '85.7', '分', '班级第 9', '', 'gold')], 'compact'))
    body.append(toolbar(['input', 'sel:全部课程', 'sel:全部状态'], '<button class="btn sm">按截止时间排序</button>'))
    body.append(table(['提交编号', '作业', '语言', '版本', '提交时间', '行数', '编译结果', '用例', '提交时效', '互评状态', '成绩', '操作'],
                      [r + ('<a href="07-submit.html">查看代码</a> · <a href="#">对比版本</a>',) for r in rows]))
    body.append(pager(4))
    return shell('student', '我的提交', '我的学习 / <b>我的提交</b>', ''.join(body))


# ============================================================ 09 结果登记
def p09():
    rows = [
        ('<span class="mono">20230114</span>', '林嘉禾', '软工2301', '2026-11-02 21:14', '33', 'v2', '5/6', '98.2', stag('已登记')),
        ('<span class="mono">20230115</span>', '王雨桐', '软工2301', '2026-11-02 20:02', '41', 'v1', '6/6', '96.5', stag('已登记')),
        ('<span class="mono">20230116</span>', '周子豪', '软工2301', '2026-11-02 19:38', '52', 'v2', '0/6', '—', stag('已登记')),
        ('<span class="mono">20230117</span>', '陈思远', '软工2301', '2026-11-02 18:51', '28', 'v1', '4/6', '99.1', stag('待登记')),
        ('<span class="mono">20230118</span>', '赵一鸣', '软工2301', '2026-11-02 17:20', '36', 'v1', '—', '—', stag('待登记')),
        ('<span class="mono">20230119</span>', '刘鹏', '软工2301', '2026-11-02 16:05', '44', 'v3', '6/6', '94.7', stag('待登记')),
    ]
    tbl = ['<table><thead><tr><th>学号</th><th>姓名</th><th>班级</th><th>提交时间</th><th>行数</th><th>版本</th>'
           '<th>用例通过</th><th>耗时(ms)</th><th>状态</th><th>操作</th></tr></thead><tbody>']
    for i, (no, nm, cls, t, ln, v, pc, ms, st) in enumerate(rows):
        tbl.append('<tr><td class="mono">' + no + '</td><td>' + nm + '</td><td>' + cls + '</td><td>' + t + '</td>'
                   '<td class="num">' + ln + '</td><td>' + v + '</td><td class="num">' + pc + '</td>'
                   '<td class="num">' + ms + '</td><td>' + st + '</td><td><a href="#">展开登记</a></td></tr>')
        if i == 2:
            tbl.append('<tr class="expand"><td colspan="10"><div class="inner">'
                       '<span><b>编译结果</b><br><select style="width:110px"><option>编译失败</option>'
                       '<option>通过</option><option>未运行</option></select></span>'
                       '<span><b>用例通过数</b><br><input value="0" style="width:84px" disabled></span>'
                       '<span><b>错误摘要（编译失败必填）</b><br>'
                       '<input value="error: cannot find symbol — 变量 errors 未声明（第 22 行）" style="width:100%"></span>'
                       '<span><button class="btn primary sm">保存</button> <button class="btn sm">取消</button></span>'
                       '</div></td></tr>')
    tbl.append('</tbody></table>')
    body = [page_head('结果登记 · 作业4：文件IO与异常处理',
                      '系统只存结果不执行代码 · 编译失败时通过数强制为 0 · 重复登记以最后一次为准',
                      '<button class="btn">' + ic('i-download', 14) + ' 下载代码包</button>'
                      '<button class="btn ghost" onclick="location.href=\'10-result-import.html\'">' + ic('i-upload', 14) + ' 批量导入</button>'
                      '<button class="btn primary">保存全部登记</button>')]
    body.append('<div class="summary">'
                '<div><div class="k">应登记</div><div class="v">86</div></div>'
                '<div><div class="k">已登记</div><div class="v">63</div></div>'
                '<div><div class="k">编译失败</div><div class="v" style="color:#B02A37">6</div></div>'
                '<div><div class="k">用例平均通过</div><div class="v">4.6 / 6</div></div></div>')
    body.append(toolbar(['input', 'sel:全部班级', 'sel:全部状态', 'txt:仅显示未登记'],
                        '<button class="btn sm">按学号排序</button><button class="btn sm">清空筛选</button>'))
    body.append('<div class="card" style="padding:0">' + ''.join(tbl) + '</div>')
    body.append(pager(86))
    return shell('ta', '结果登记', '批改与登记 / <b>结果登记</b>', ''.join(body))


# ============================================================ 10 批量导入
def p10():
    ok = [('20230117', '陈思远', '通过', '4', '—'), ('20230118', '赵一鸣', '通过', '6', '—'),
          ('20230119', '刘鹏', '通过', '6', '—'), ('20230120', '孙悦', '编译失败', '0', '缺少 import java.util.List')]
    bad = [('20230121', '郑凯', '用例数 7 超过作业上限 6', '第 5 行'),
           ('20230999', '不存在', '学号不在本课程选课名单', '第 6 行'),
           ('20230123', '何静', '编译结果「未通过」不在字典内', '第 7 行')]
    body = [page_head('批量导入运行结果', '支持按学号批量导入，导入失败逐行返回原因，不允许整批静默失败',
                      '<button class="btn">下载导入模板</button>'
                      '<button class="btn primary" onclick="location.href=\'09-result-entry.html\'">确认导入 4 条</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:12px">')
    body.append('<div class="card"><h2>上传结果文件</h2>'
                '<div class="imgs" style="align-items:center">'
                '<div class="upload filled" style="width:120px;height:120px">' + ic('i-file', 22)
                + '<div style="margin-top:6px;font-size:11px">result_45.xlsx<br>18.2 KB</div></div>'
                '<div style="font-size:12px;color:#5B6770;line-height:2">'
                '文件：<b>result_45.xlsx</b>（7 行数据）<br>'
                '已校验表头：学号 / 姓名 / 编译结果 / 通过数 / 错误摘要<br>'
                '大小 18.2 KB，未超过 20 MB 上限<br><a href="#">重新上传</a></div></div>'
                '<div style="margin-top:14px;display:flex;gap:10px">'
                '<button class="btn">仅校验不导入</button><button class="btn primary">导入 4 条成功记录</button></div></div>')
    body.append('<div class="card" style="padding:0"><div style="padding:14px 16px 0"><h2 style="margin:0 0 10px">导入结果'
                '<span class="tag t-green" style="margin-left:8px">成功 4</span>'
                '<span class="tag t-red" style="margin-left:6px">失败 3</span></h2></div>'
                + table(['学号', '姓名', '编译结果', '通过数', '错误摘要', '结果'], [
                    (('<span class="mono">' + a + '</span>'), b, stag(c) if c in ('通过', '编译失败') else c, d, e,
                     '<span class="tag t-green">已导入</span>') for a, b, c, d, e in ok]
                    + [(('<span class="mono">' + a + '</span>'), b, '—', '—', row, '<span class="tag t-red">失败：' + row + '</span>')
                       for a, b, row, _ in bad])
                + '</div>')
    body.append('</div>')
    body.append('<div style="width:320px;flex:0 0 320px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>导入规则</h2>' + note(
                    '1. 表头必须为「学号 / 编译结果 / 通过数 / 错误摘要」<br>'
                    '2. 通过数不得超过作业用例总数（6）<br>'
                    '3. 编译失败时通过数一律按 0 落库<br>'
                    '4. 学号必须在本课程选课名单内<br>'
                    '5. 同一学号重复出现时以最后一行为准并记录修改前后值', 'info') + '</div>'
                '<div class="card"><h2>最近导入</h2>' + timeline([
                    ('ok', '导入 41 条成功', '周敏', '11-02 19:40'),
                    ('warn', '导入 38 条，失败 3 条', '周敏', '10-26 20:11'),
                    ('ok', '导入 44 条成功', '吴迪', '10-19 21:02'),
                ]) + '</div></div>')
    body.append('</div>')
    return shell('ta', '批量导入', '批改与登记 / <b>批量导入</b>', ''.join(body))


PAGES = [('01-login', p01), ('02-dash', p02), ('03-course', p03), ('04-assignment-new', p04),
         ('05-assignment-list', p05), ('06-assignment-detail', p06), ('07-submit', p07),
         ('08-my-submissions', p08), ('09-result-entry', p09), ('10-result-import', p10)]
