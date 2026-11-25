#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面 11-19：互评分配 / 互评工作台 / 互评进度 / 查重记录 / 查重复核 / 成绩审核 / 成绩汇总 / 申诉处理 / 可信度明细"""
from base import *

JAVA_A = ['public class TicketSeller {', '    private int ticket = 100;', '',
          '    public void sell() {', '        synchronized (this) {',
          '            if (ticket > 0) {', '                ticket--;',
          '                System.out.println(Thread.currentThread().getName()',
          '                    + " 卖出 1 张，剩余 " + ticket);', '            }',
          '        }', '    }', '}']
JAVA_B = ['public class TicketSeller {', '    private int ticket = 100;', '',
          '    public void sell() {', '        synchronized (this) {',
          '            if (ticket > 0) {', '                ticket = ticket - 1;',
          '                System.out.println(Thread.currentThread().getName()',
          '                    + " sold one, left " + ticket);', '            }',
          '        }', '    }', '}']


# ============================================================ 11 互评分配
def p11():
    rows = [
        ('林嘉禾', '<span class="mono">20230114</span>', '88.4', '王雨桐、周子豪、陈思远'),
        ('王雨桐', '<span class="mono">20230115</span>', '88.4', '周子豪、陈思远、赵一鸣'),
        ('周子豪', '<span class="mono">20230116</span>', '88.4', '陈思远、赵一鸣、刘鹏'),
        ('陈思远', '<span class="mono">20230117</span>', '88.4', '赵一鸣、刘鹏、孙悦'),
        ('赵一鸣', '<span class="mono">20230118</span>', '88.4', '刘鹏、孙悦、郑凯'),
        ('刘鹏', '<span class="mono">20230119</span>', '88.4', '孙悦、郑凯、林嘉禾'),
    ]
    body = [page_head('互评分配 · 作业3：多线程售票模拟',
                      '轮转算法 · 禁止自评 · 每人被评次数偏差不超过 1 次 · 确认后不可重排',
                      '<button class="btn">重新生成</button><button class="btn primary">确认分配并通知</button>')]
    body.append('<div class="summary">'
                '<div><div class="k">参与互评的提交</div><div class="v">84 份</div></div>'
                '<div><div class="k">互评份数</div><div class="v">3 份 / 人</div></div>'
                '<div><div class="k">生成任务</div><div class="v">252 条</div></div>'
                '<div><div class="k">被评次数偏差</div><div class="v" style="color:#2B7955">0</div></div></div>')
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:12px">')
    body.append(toolbar(['sel:按学号顺序', 'input', 'txt:轮次 1 / 共 1 轮'],
                        '<button class="btn sm">导出分配表</button>'))
    body.append('<div class="card" style="padding:0">'
                + table(['评阅人', '学号', '分配均衡度', '本轮被评对象（匿名展示）'], [
                    (r[0], r[1], '<span class="tag t-green">' + r[2] + '</span>', r[3]) for r in rows])
                + '</div>')
    body.append(pager(84))
    body.append('</div>')
    body.append('<div style="width:340px;flex:0 0 340px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>分配校验</h2>'
                + note('<b style="color:#2B7955">✓</b> 无自评任务（评阅人 ≠ 作者）<br>'
                       '<b style="color:#2B7955">✓</b> 同一提交未被同一人重复分配<br>'
                       '<b style="color:#2B7955">✓</b> 每人被评 3 次，偏差 0<br>'
                       '<b style="color:#C2703A">!</b> 2 份提交未参与（未提交学生已排除）', 'ok')
                + '</div>'
                '<div class="card"><h2>被评次数分布</h2>'
                + chart_vbars([('2 次', 2), ('3 次', 78), ('4 次', 4)], 1) + '</div>'
                '<div class="card"><h2>分配说明</h2>'
                '<div class="kv" style="grid-template-columns:74px 1fr;font-size:12px">'
                '<div style="color:#5B6770;text-align:right">算法</div><div>轮转 + 错位偏移</div>'
                '<div style="color:#5B6770;text-align:right">匿名</div><div>是（隐藏姓名与学号）</div>'
                '<div style="color:#5B6770;text-align:right">互评截止</div><div>2026-11-09 23:59</div>'
                '<div style="color:#5B6770;text-align:right">已开始评分</div><div>不可重排，只能追加补评</div>'
                '</div></div></div>')
    body.append('</div>')
    return shell('teacher', '互评分配', '互评与查重 / <b>互评分配</b>', ''.join(body))


# ============================================================ 12 互评工作台
def p12():
    body = [page_head('互评工作台 · 匿名提交 #A-1073',
                      '作业3：多线程售票模拟 · 剩余 5 天 17 小时 · 你还有 2 份待评',
                      '<span class="tag t-blue" style="align-self:center">匿名展示 · 不可查看作者</span>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:stretch">')
    # 左：代码
    body.append('<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:10px">'
                '<div class="card" style="flex:1;display:flex;flex-direction:column">'
                '<div class="code-head">' + ic('i-lock', 14) + '<span class="fnm">TicketSeller.java</span>'
                '<span class="tag t-gray">匿名 #A-1073</span>'
                '<span class="spacer"></span><span style="font-size:11px;color:#5B6770">13 行 · Java</span></div>'
                + code_block(JAVA_A, 1, (5, 6, 7), 'h420')
                + '<div style="margin-top:10px;font-size:12px;color:#5B6770;line-height:1.9">'
                '同行对比提示：该提交与 #A-1091 相似度 82.4%，命中「变量重命名 + 结构一致」，'
                '系统已生成待复核记录，互评时请只评代码质量，不要讨论作者。</div></div></div>')
    # 右：打分
    body.append('<div style="width:392px;flex:0 0 392px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>三维度打分</h2>'
                '<div class="slider-row"><span class="nm">正确性<small>0–60 分</small></span>'
                '<span class="track"><i style="width:78%"></i><b style="left:calc(78% - 8px)"></b></span>'
                '<span class="vv">47.0<small>/60</small></span></div>'
                '<div class="slider-row"><span class="nm">可读性<small>0–25 分</small></span>'
                '<span class="track"><i style="width:72%"></i><b style="left:calc(72% - 8px)"></b></span>'
                '<span class="vv">18.0<small>/25</small></span></div>'
                '<div class="slider-row"><span class="nm">代码规范<small>0–15 分</small></span>'
                '<span class="track"><i style="width:73%"></i><b style="left:calc(73% - 8px)"></b></span>'
                '<span class="vv">11.0<small>/15</small></span></div>'
                '<div style="border-top:1px solid #DEE4E9;margin-top:6px;padding-top:12px;display:flex;justify-content:space-between;align-items:baseline">'
                '<span style="font-size:13px;color:#5B6770">互评总分（三项之和）</span>'
                '<span style="font-size:26px;font-weight:600;font-variant-numeric:tabular-nums">76.0</span></div></div>'
                '<div class="card"><h2>评语 <span style="font-size:12px;font-weight:400;color:#5B6770">'
                '<span id="cc">41</span> / 最少 20 字</span></h2>'
                '<textarea style="width:100%;height:112px">synchronized 块的范围写得对，但第 7 行先判断 ticket > 0 '
                '再自减，若两个线程同时进入仍可能出现负数，建议改成 AtomicInteger 或加锁后重新检查。'
                '第 9 行字符串拼接跨行对齐不好读，建议用 String.format。</textarea>'
                + '<div style="margin-top:10px">' + note('评语已引用具体行号，判定为有效评语；'
                                                       '若少于 20 字或只有「写得不错」将被标记为低质量并扣可信度分。', 'ok') + '</div></div>'
                '<div class="card"><div style="display:flex;gap:10px">'
                '<button class="btn" style="flex:1">存为草稿</button>'
                '<button class="btn primary" style="flex:1">提交评分</button></div>'
                '<div style="font-size:12px;color:#5B6770;margin-top:9px">提交后双方均不可修改；'
                '如需重评只能由教师追加补评任务。</div></div></div>')
    body.append('</div>')
    return shell('student', '互评工作台', '互评 / <b>待我评</b>', ''.join(body))


# ============================================================ 13 互评进度
def p13():
    rows = [
        ('林嘉禾', '<span class="mono">20230114</span>', '3', '3', '0', '—', '100%', stag('已评')),
        ('王雨桐', '<span class="mono">20230115</span>', '3', '2', '1', '—', '67%', stag('待评')),
        ('周子豪', '<span class="mono">20230116</span>', '3', '3', '0', '—', '100%', stag('已评')),
        ('陈思远', '<span class="mono">20230117</span>', '3', '2', '1', '11-09 23:59', '67%', stag('待评')),
        ('赵一鸣', '<span class="mono">20230118</span>', '3', '0', '3', '11-09 23:59', '0%', stag('待评')),
        ('刘鹏', '<span class="mono">20230119</span>', '3', '3', '0', '—', '100%', stag('已评')),
        ('孙悦', '<span class="mono">20230120</span>', '3', '1', '2', '11-09 23:59', '33%', stag('待评')),
    ]
    body = [page_head('互评进度 · 作业3：多线程售票模拟',
                      '84 份提交 · 252 条任务 · 完成 207 条（82.1%）· 截止 2026-11-09 23:59',
                      '<button class="btn">导出未评名单</button><button class="btn gold">一键催评 6 人</button>')]
    body.append(kpis([('任务总数', '252', '条', '', '', ''), ('已完成', '207', '条', '', '', ''),
                      ('待评', '41', '条', '', '', ''), ('超时未评', '4', '条', '影响可信度', 'down', ''),
                      ('低质量评语', '9', '条', '占已评 4.3%', 'down', ''), ('平均评语字数', '58', '字', '', '', '')], 'compact'))
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0">' + toolbar(['input', 'sel:全部状态', 'sel:全部班级'],
                                                             '<button class="btn sm">按未评数排序</button>')
                + '<div class="card" style="padding:0">'
                + table(['评阅人', '学号', '应评', '已评', '未评', '截止/超时', '完成率', '状态', '操作'],
                        [r + ('<a href="#">催评</a> · <a href="#">查看评语</a>',) for r in rows])
                + '</div>' + pager(84) + '</div>')
    body.append('<div style="width:400px;flex:0 0 400px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>近 7 天完成趋势</h2>'
                + chart_line(['10-27', '10-28', '10-29', '10-30', '10-31', '11-01', '11-02'],
                             [('累计完成', '#3D7A66', [12, 38, 71, 104, 143, 178, 207]),
                              ('当日新增', '#C2703A', [12, 26, 33, 33, 39, 35, 29])], 146, 330)
                + '</div>'
                '<div class="card"><h2>评语质量分布</h2>'
                + chart_hbars([('≥50 字且引用行号', 128, '128'), ('≥50 字无行号', 44, '44'),
                               ('20–49 字', 26, '26'), ('<20 字', 6, '6'), ('疑似套话', 3, '3')], 108)
                + '</div>'
                '<div style="font-size:11.5px;color:#5B6770;line-height:1.7;padding:0 2px">'
                '超时未评计入完成率，完成率因子按 0 计；教师可补评或标记「已豁免」。</div></div>')
    body.append('</div>')
    return shell('teacher', '互评进度', '互评与查重 / <b>互评进度</b>', ''.join(body))


# ============================================================ 14 查重记录
def p14():
    rows = [
        ('<span class="mono">CD2611020031</span>', '#A-1073 / #A-1091', '82.40%', '变量重命名 + 结构一致',
         '<span class="mono" style="font-size:11px">第 5–11 行 · 7 行相同</span>', stag('待复核'), '11-02 22:10'),
        ('<span class="mono">CD2611020030</span>', '#A-1042 / #A-1055', '91.20%', '完全一致',
         '<span class="mono" style="font-size:11px">第 1–19 行 · 19 行相同</span>', stag('待复核'), '11-02 22:10'),
        ('<span class="mono">CD2611020029</span>', '#A-1088 / #A-1120', '76.80%', '注释雷同',
         '<span class="mono" style="font-size:11px">第 12–20 行 · 9 行相同</span>', stag('待复核'), '11-02 22:10'),
        ('<span class="mono">CD2611020028</span>', '#A-1003 / #A-1017', '73.10%', '结构一致',
         '<span class="mono" style="font-size:11px">第 4–10 行 · 7 行相同</span>', stag('待复核'), '11-02 22:10'),
        ('<span class="mono">CD2610260014</span>', '#A-0921 / #A-0940', '88.90%', '完全一致',
         '<span class="mono" style="font-size:11px">第 2–16 行 · 15 行相同</span>', stag('确认抄袭'), '10-26 21:30'),
        ('<span class="mono">CD2610260013</span>', '#A-0876 / #A-0899', '74.20%', '变量重命名',
         '<span class="mono" style="font-size:11px">第 8–14 行 · 7 行相同</span>', stag('误报'), '10-26 21:30'),
        ('<span class="mono">CD2610190007</span>', '#A-0702 / #A-0731', '79.50%', '结构一致',
         '<span class="mono" style="font-size:11px">第 3–11 行 · 9 行相同</span>', stag('需面谈'), '10-19 20:40'),
    ]
    body = [page_head('查重记录 · 作业3：多线程售票模拟',
                      '检测批次 CD-B2601102-07 · 规则版本 v1.2 · 84 份提交两两比对（3 486 对）· 生成 12 条待复核',
                      '<button class="btn">重跑检测</button><button class="btn ghost">' + ic('i-export', 14) + ' 导出</button>'
                      '<button class="btn primary">批量确认误报</button>')]
    body.append(kpis([('比对对数', '3 486', '对', '', '', ''), ('命中 ≥70%', '12', '条', '', '', ''),
                      ('待复核', '4', '条', '超 24 小时未处理 1 条', 'down', 'gold'), ('已确认抄袭', '3', '条', '', '', ''),
                      ('误报', '2', '条', '误报率 16.7%', '', ''), ('检测耗时', '0.8', 's', '', '', '')], 'compact'))
    body.append('<div class="tabs"><a class="on" href="#">全部<span class="c">12</span></a>'
                '<a href="#">待复核<span class="c">4</span></a><a href="#">确认抄袭<span class="c">3</span></a>'
                '<a href="#">误报<span class="c">2</span></a><a href="#">需面谈<span class="c">3</span></a></div>')
    body.append(toolbar(['input', 'sel:全部作业', 'sel:全部结论', 'sel:相似度 ≥70%'],
                        '<button class="btn sm">按相似度排序</button>'))
    tbl = ['<table><thead><tr><th>记录编号</th><th>提交对</th><th>相似度</th><th>命中类型</th><th>证据片段</th>'
           '<th>复核结论</th><th>检测时间</th><th>操作</th></tr></thead><tbody>']
    for i, r in enumerate(rows):
        cls = ' style="background:#FCF6EF"' if i == 0 else ''
        tbl.append('<tr' + cls + '>' + ''.join('<td>' + c + '</td>' for c in r)
                   + '<td><a href="15-similarity-review.html">复核</a> · <a href="#">对比</a></td></tr>')
    tbl.append('</tbody></table>')
    body.append('<div class="card" style="padding:0">' + ''.join(tbl) + '</div>')
    body.append(pager(12))
    # 右侧抽屉：并排两段代码
    drawer = ['<div class="mask"></div><div class="drawer wide">'
              '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">'
              '<h3 style="margin:0">CD2611020031 · 相似度 82.40%</h3>'
              '<span class="tag t-amber">待复核</span></div>'
              '<div style="font-size:12px;color:#5B6770;margin-bottom:10px">'
              '命中规则：标识符归一化后一致 11 行 / 注释剥离后结构一致 7 行 · 规则版本 v1.2</div>'
              '<div class="cmp"><div><div class="col-h"><span>#A-1073（较早提交）</span><span>2026-11-02 21:14</span></div>'
              + code_block(JAVA_A, 5, (7, 8, 9), 'h240') + '</div>'
              '<div><div class="col-h"><span>#A-1091</span><span>2026-11-02 21:52</span></div>'
              + code_block(JAVA_B, 5, (7, 8, 9), 'h240') + '</div></div>'
              '<div style="margin-top:12px">' + note('差异点：第 7 行 ticket-- 与 ticket = ticket - 1；'
                                                   '第 9 行输出文案「卖出 1 张，剩余」与「sold one, left」。'
                                                   '标识符归一化后两段完全相同，判定为变量重命名式抄袭。', 'warn') + '</div>'
              '<div style="display:flex;gap:10px;margin-top:14px">'
              '<button class="btn" style="flex:1">标记误报</button>'
              '<button class="btn" style="flex:1">需面谈</button>'
              '<button class="btn danger" style="flex:1">确认抄袭（需两名教师）</button></div></div>']
    body.append(''.join(drawer))
    return shell('ta', '查重记录', '查重 / <b>查重记录</b>', ''.join(body))


# ============================================================ 15 查重复核
def p15():
    body = [page_head('相似度复核 · CD2611020031',
                      '#A-1073 / #A-1091 · 相似度 82.40% · 命中「变量重命名 + 结构一致」· 规则 v1.2',
                      '<button class="btn">标记误报</button><button class="btn">需面谈</button>'
                      '<button class="btn danger">确认抄袭</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start;margin-bottom:14px">')
    body.append('<div style="flex:1;min-width:0">'
                '<div class="cmp"><div><div class="col-h"><span class="fnm" style="font-family:var(--mono)">#A-1073</span>'
                '<span style="color:#5B6770">林嘉禾 · 21:14 提交 · 88 行</span></div>'
                + code_block(JAVA_A, 5, (7, 8, 9), 'h300') + '</div>'
                '<div><div class="col-h"><span class="fnm" style="font-family:var(--mono)">#A-1091</span>'
                '<span style="color:#5B6770">王雨桐 · 21:52 提交 · 90 行</span></div>'
                + code_block(JAVA_B, 5, (7, 8, 9), 'h300') + '</div></div></div>')
    body.append('<div style="width:308px;flex:0 0 308px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>命中明细</h2>'
                '<div class="score-row"><span>标识符相似度 <span style="color:#8FA099">×0.3</span></span>'
                '<span class="bar"><i style="width:94%;background:#B02A37"></i></span><span class="v">94.0</span></div>'
                '<div class="score-row"><span>结构相似度 <span style="color:#8FA099">×0.5</span></span>'
                '<span class="bar"><i style="width:81%;background:#C2703A"></i></span><span class="v">81.0</span></div>'
                '<div class="score-row"><span>注释相似度 <span style="color:#8FA099">×0.2</span></span>'
                '<span class="bar"><i style="width:60%"></i></span><span class="v">60.0</span></div>'
                '<div style="border-top:1px solid #DEE4E9;margin-top:8px;padding-top:10px;display:flex;justify-content:space-between">'
                '<span style="font-size:13px;color:#5B6770">加权相似度</span>'
                '<b style="font-size:20px;color:#B02A37">82.40%</b></div></div>'
                '<div class="card"><h2>证据片段</h2>'
                '<div class="sim-line same">    if (ticket &gt; 0) {</div>'
                '<div class="sim-line same">        ticket--;</div>'
                '<div class="sim-line same">        System.out.println(Thread.currentThread().getName()</div>'
                '<div class="sim-line same">            + " 卖出 1 张，剩余 " + ticket);</div>'
                '<div style="font-size:12px;color:#5B6770;margin-top:8px">第 5–11 行，共 7 行归一化后完全一致；'
                '第 9 行文案差异在变量重命名规则下被归一。</div></div>'
                '<div class="card"><h2>复核要求</h2>'
                + note('确认抄袭需两名教师签字，且通知学生；误报需填写理由（≥10 字）；'
                       '判断不了时选「需面谈」，学生可在 3 天内提交说明。', 'info') + '</div></div>')
    body.append('</div>')
    return shell('ta', '相似度复核', '查重 / <b>相似度复核</b>', ''.join(body))


# ============================================================ 16 成绩审核
def p16():
    rows = [
        ('<span class="mono">20230114</span>', '林嘉禾', '83.3', '81.6', '86.0', '83.9', '高可信 92.5', '—', stag('未发布')),
        ('<span class="mono">20230115</span>', '王雨桐', '100', '88.4', '90.0', '92.9', '高可信 88.1', '—', stag('未发布')),
        ('<span class="mono">20230116</span>', '周子豪', '0', '—', '52.0', '10.8', '低可信 38.4', '<span style="color:#B02A37">互评不计入</span>', stag('未发布')),
        ('<span class="mono">20230117</span>', '陈思远', '66.7', '74.2', '70.0', '70.1', '中可信 71.6', '<span style="color:#9C5C12">×0.8 折算</span>', stag('未发布')),
        ('<span class="mono">20230118</span>', '赵一鸣', '100', '—', '88.0', '94.0', '<span style="color:#B02A37">未完成互评</span>', '<span style="color:#B02A37">扣 5 分</span>', stag('未发布')),
        ('<span class="mono">20230119</span>', '刘鹏', '100', '92.6', '94.0', '95.4', '高可信 95.2', '—', stag('未发布')),
        ('<span class="mono">20230120</span>', '孙悦', '50.0', '68.0', '64.0', '60.2', '中可信 69.0', '<span style="color:#9C5C12">×0.8 折算</span>', stag('未发布')),
        ('<span class="mono">20230121</span>', '郑凯', '66.7', '—', '58.0', '62.5', '<span style="color:#B02A37">超时未评</span>', '<span style="color:#B02A37">需补评</span>', stag('未发布')),
    ]
    body = [page_head('成绩审核 · 作业3：多线程售票模拟',
                      '运行结果 40% + 互评 25% + 教师 35% = 100% · 已合成 84 条，其中 6 条需人工确认',
                      '<button class="btn">重算全部</button><button class="btn">导出核算表</button>'
                      '<button class="btn primary">发布成绩</button>')]
    body.append('<div class="card" style="padding:12px 18px;margin-bottom:14px;display:flex;gap:26px;align-items:center;flex-wrap:wrap">'
                '<span style="font-size:13px;color:#5B6770">权重设置</span>'
                '<span style="display:flex;align-items:center;gap:6px;font-size:13px">运行结果 <input value="40" style="width:64px"> %</span>'
                '<span style="display:flex;align-items:center;gap:6px;font-size:13px">互评 <input value="25" style="width:64px"> %</span>'
                '<span style="display:flex;align-items:center;gap:6px;font-size:13px">教师 <input value="35" style="width:64px"> %</span>'
                '<span class="tag t-green">合计 100%</span>'
                '<span style="flex:1"></span>'
                '<span style="font-size:12px;color:#5B6770">改分需填写不少于 10 字说明，发布后只能走申诉</span></div>')
    body.append(toolbar(['input', 'sel:全部可信度等级', 'txt:仅看需人工确认的 6 条'],
                        '<button class="btn sm">按最终成绩排序</button><button class="btn sm">批量调整</button>'))
    body.append('<div class="card" style="padding:0">'
                + table(['学号', '姓名', '运行结果分', '互评分', '教师评分', '最终成绩', '互评可信度', '系统调整', '发布状态', '操作'],
                        [r + ('<a href="19-credibility.html">可信度明细</a> · <a href="#">改分</a>',) for r in rows])
                + '</div>')
    body.append(pager(84))
    return shell('teacher', '成绩审核', '成绩 / <b>成绩审核</b>', ''.join(body))


# ============================================================ 17 成绩汇总
def p17():
    rows = [
        ('1', '王雨桐', '<span class="mono">20230115</span>', '100', '88.4', '90.0', '<b>92.9</b>', '高可信', stag('已发布')),
        ('2', '刘鹏', '<span class="mono">20230119</span>', '100', '92.6', '94.0', '<b>95.4</b>', '高可信', stag('已发布')),
        ('3', '林嘉禾', '<span class="mono">20230114</span>', '83.3', '81.6', '86.0', '<b>83.9</b>', '高可信', stag('已发布')),
        ('4', '陈思远', '<span class="mono">20230117</span>', '66.7', '74.2', '70.0', '<b>70.1</b>', '中可信', stag('未发布')),
        ('5', '孙悦', '<span class="mono">20230120</span>', '50.0', '68.0', '64.0', '<b>60.2</b>', '中可信', stag('未发布')),
        ('6', '郑凯', '<span class="mono">20230121</span>', '66.7', '—', '58.0', '<b>62.5</b>', '低可信', stag('未发布')),
        ('7', '周子豪', '<span class="mono">20230116</span>', '0', '—', '52.0', '<b>10.8</b>', '低可信', stag('未发布')),
    ]
    body = [page_head('成绩汇总 · Java程序设计（软工2301）',
                      '86 名学生 · 6 次作业加权 · 统计口径与明细一致 · 导出记录导出入与时间',
                      '<button class="btn ghost">' + ic('i-export', 14) + ' 导出成绩单</button>'
                      '<button class="btn">导出互评明细</button>')]
    body.append(kpis([('平均分', '78.4', '分', '↑ 2.1', '', ''), ('及格率', '91.9', '%', '79/86', '', ''),
                      ('优秀率', '18.6', '%', '≥90 分', '', ''), ('用例平均通过', '4.6', '/ 6', '', '', ''),
                      ('互评完成率', '82.4', '%', '6 人未评', 'down', ''), ('相似度告警', '12', '条', '3 条确认', 'down', '')], 'compact'))
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0">' + toolbar(['input', 'sel:作业3', 'sel:软工2301', 'sel:全部发布状态'],
                                                             '<button class="btn sm">按成绩排序</button>')
                + '<div class="card" style="padding:0">'
                + table(['排名', '姓名', '学号', '运行结果', '互评', '教师', '最终成绩', '可信度', '发布状态', '操作'],
                        [r + ('<a href="18-appeal.html">申诉</a> · <a href="#">改分</a>',) for r in rows])
                + '</div>' + pager(86) + '</div>')
    body.append('<div style="width:360px;flex:0 0 360px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>分数段分布</h2>'
                + chart_vbars([('90+', 16), ('80-89', 29), ('70-79', 22), ('60-69', 12), ('<60', 7)], 2) + '</div>'
                '<div class="card"><h2>各题平均分</h2>'
                + chart_hbars([('作业1', 86.2, '86.2'), ('作业2', 82.4, '82.4'), ('作业3', 78.4, '78.4'),
                               ('作业4', 74.1, '74.1'), ('作业5', 71.6, '71.6'), ('作业6', 68.9, '68.9')], 56) + '</div>'
                '<div class="card"><h2>导出口径</h2>'
                + note('导出文件：成绩单_软工2301_作业3.xlsx · 86 行 · 由陈立群于 11-02 07:20 导出；'
                       '发布后改分必须走申诉流程，导出内容与明细实时一致。', 'info') + '</div></div>')
    body.append('</div>')
    return shell('teacher', '成绩汇总', '成绩 / <b>成绩汇总</b>', ''.join(body))


# ============================================================ 18 申诉处理
def p18():
    rows = [
        ('<span class="mono">SS2611020003</span>', '林嘉禾', '互评分偏低', '作业3', '86.0', '81.6', '待受理', '11-02 06:12'),
        ('<span class="mono">SS2611020002</span>', '孙悦', '成绩计算错误', '作业2', '—', '82.0', '处理中', '11-02 05:40'),
        ('<span class="mono">SS2611010007</span>', '王雨桐', '查重误判', '作业3', '—', '88.4', '需重新互评', '11-01 22:30'),
        ('<span class="mono">SS2611010005</span>', '陈思远', '互评分偏低', '作业2', '74.2', '78.0', '已改分', '11-01 19:02'),
        ('<span class="mono">SS2610310011</span>', '郑凯', '成绩计算错误', '作业1', '—', '62.0', '已驳回', '10-31 21:14'),
    ]
    body = [page_head('申诉处理 · 作业3',
                      '学生需在成绩发布后 7 天内提交 · 由非原评分教师处理 · 已受理 5 条，待处理 2 条',
                      '<button class="btn">导出申诉台账</button><button class="btn primary">批量分派</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0">' + toolbar(['input', 'sel:全部类型', 'sel:全部状态', 'sel:全部作业'])
                + '<div class="card" style="padding:0">'
                + table(['申诉编号', '学生', '类型', '作业', '原互评分', '原成绩', '状态', '提交时间', '操作'],
                        [(r[0], r[1], r[2], r[3], r[4], r[5], stag(r[6]), r[7], '<a href="#">处理</a> · <a href="#">证据</a>')
                         for r in rows])
                + '</div>' + pager(5) + '</div>')
    body.append('<div class="card" style="width:430px;flex:0 0 430px">'
                '<h2 style="display:flex;justify-content:space-between;align-items:center">SS2611020003'
                '<span class="tag t-amber">待受理</span></h2>'
                '<div class="kv" style="grid-template-columns:78px 1fr;margin-bottom:12px">'
                '<div style="color:#5B6770;text-align:right">学生</div><div>林嘉禾 · 20230114</div>'
                '<div style="color:#5B6770;text-align:right">类型</div><div>互评分偏低</div>'
                '<div style="color:#5B6770;text-align:right">成绩</div><div>81.6 → 期望 86.0</div>'
                '<div style="color:#5B6770;text-align:right">原评分教师</div><div>陈立群（不可处理本条）</div></div>'
                '<div style="font-size:12px;color:#5B6770;margin-bottom:6px">申诉理由（62 字）</div>'
                '<div style="background:#F7F9F8;border:1px solid #DEE4E9;border-radius:6px;padding:10px 12px;font-size:13px;line-height:1.9">'
                '我的提交第 20–22 行实现了 errors 列表，但互评人给的可读性只有 12 分且评语只说「代码略乱」，'
                '没有指出具体位置。同一份作业的另外两个评阅人都给了 20 分以上，希望复核这项评分是否合理。</div>'
                '<div style="font-size:12px;color:#5B6770;margin:12px 0 6px">证据附件（2）</div>'
                '<div class="imgs"><div class="img-box" style="width:104px;height:76px">互评评语截图</div>'
                '<div class="img-box" style="width:104px;height:76px">代码对比截图</div></div>'
                '<div style="font-size:12px;color:#5B6770;margin:14px 0 6px">处理方式</div>'
                '<select style="width:100%;margin-bottom:10px"><option>重新互评（追加一轮评阅人）</option>'
                '<option>驳回</option><option>改分</option></select>'
                '<textarea style="width:100%;height:76px" placeholder="处理意见必填（≥10 字），改分需写明新分数与依据"></textarea>'
                '<div style="display:flex;gap:8px;margin-top:10px">'
                '<button class="btn" style="flex:1">退回补充</button>'
                '<button class="btn primary" style="flex:1">提交处理</button></div></div>')
    body.append('</div>')
    return shell('teacher', '申诉处理', '成绩 / <b>申诉处理</b>', ''.join(body))


# ============================================================ 19 可信度明细
def p19():
    body = [page_head('互评可信度明细 · 林嘉禾 / 作业3',
                      '一次互评是否可信由 5 项因素计算 · 可信度分 92.5（高可信）· 规则版本 v2.1',
                      '<button class="btn">重算</button><button class="btn ghost">导出计算说明</button>'
                      '<button class="btn">人工修正</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start;gap:14px">')
    body.append('<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:12px">')
    body.append('<div class="card"><h2>因素得分（权重合计 100%）</h2>'
                + score_rows([
                    ('评分偏离度', '25%', 96.0, ''),
                    ('评语有效长度', '20%', 100.0, ''),
                    ('互评完成率', '20%', 92.0, ''),
                    ('提交相似度', '20%', 78.0, ''),
                    ('评分一致性', '15%', 94.0, ''),
                ])
                + '<div style="border-top:1px solid #DEE4E9;margin-top:6px;padding-top:12px;display:flex;justify-content:space-between;align-items:baseline">'
                '<span style="font-size:13px;color:#5B6770">可信度分 = Σ（单项归一化得分 × 权重）</span>'
                '<span><b style="font-size:26px;color:#2B7955">92.5</b><span class="tag t-green" style="margin-left:8px">高可信</span></span></div></div>')
    body.append('<div class="card"><h2>输入快照（计算时冻结，后续数据变化不影响本结果）</h2>'
                + table(['因素', '原始输入', '归一化得分', '权重', '贡献分'], [
                    ('评分偏离度', '该互评 86.0 · 教师均分 87.4 · 偏差 1.4', '96.0', '25%', '24.00'),
                    ('评语有效长度', '去套话后 61 字 · 引用行号 3 处', '100.0', '20%', '20.00'),
                    ('互评完成率', '近 3 次任务 3/3 按期完成（100%）', '92.0', '20%', '18.40'),
                    ('提交相似度', '该生最高相似度 73.1%（#A-1017）', '78.0', '20%', '15.60'),
                    ('评分一致性', '同提交 3 份互评标准差 4.2 → 94 分', '94.0', '15%', '14.10'),
                ]) + '<div style="margin-top:10px">' + note('教师评分未出时，评分偏离度用同作业互评中位数代入并标注；'
                                                          '无查重记录时该项不计分，权重按剩余项归一。', 'info') + '</div></div>')
    body.append('<div class="card"><h2>结论与可改进项</h2>'
                + '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px">'
                '<div><div style="font-size:13px;font-weight:500;margin-bottom:8px">主要影响因素</div>'
                '<div style="font-size:12.5px;color:#5B6770;line-height:2">'
                '· 评语 61 字且引用 3 处行号，是本条互评最可信的依据<br>'
                '· 三项得分与教师评分偏差仅 1.4 分<br>'
                '· 同一提交 3 份互评标准差 4.2，一致性良好</div></div>'
                '<div><div style="font-size:13px;font-weight:500;margin-bottom:8px">未满足条件 / 可改进</div>'
                '<div style="font-size:12.5px;color:#5B6770;line-height:2">'
                '· 提交相似度 73.1%，虽未达抄袭线但需关注<br>'
                '· 该生 2 次提交版本差异较大，建议教师抽查<br>'
                '· 未触发任何降级条件</div></div></div>'
                '<div style="margin-top:12px">' + note('<b>本次结论</b>：高可信 → 互评分 86.0 按 25% 权重直接计入最终成绩。', 'ok') + '</div></div>')
    body.append('</div>')
    body.append('<div style="width:330px;flex:0 0 330px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>等级分布</h2>' + chart_donut(
                    [('高可信', 62, '#1E4B3F'), ('中可信', 28, '#3D7A66'), ('较低', 8, '#C2703A'), ('低可信', 2, '#B02A37')],
                    '92.5', '本条', 118) + '</div>'
                '<div class="card"><h2>同分互评对比</h2>'
                + chart_hbars([('#A-1073', 92.5, '92.5'), ('#A-1042', 88.1, '88.1'), ('#A-1088', 71.6, '71.6'),
                               ('#A-1003', 58.4, '58.4'), ('#A-0921', 38.4, '38.4')], 66) + '</div>'
                '<div class="card"><h2>人工修正留痕</h2>' + timeline([
                    ('ok', '系统计算 92.5（高可信）', '规则 v2.1', '11-02 06:40'),
                    ('', '待教师确认或修正', '陈立群', '—'),
                ]) + '<div style="font-size:12px;color:#5B6770;margin-top:6px">修正需填写不少于 5 字原因，原始结果保留不覆盖。</div></div></div>')
    body.append('</div>')
    return shell('ta', '可信度明细', '可信度 / <b>可信度明细</b>', ''.join(body))


PAGES = [('11-peer-assign', p11), ('12-peer-desk', p12), ('13-peer-progress', p13),
         ('14-similarity-list', p14), ('15-similarity-review', p15), ('16-score-review', p16),
         ('17-score-summary', p17), ('18-appeal', p18), ('19-credibility', p19)]
