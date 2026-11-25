#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""编程课程作业提交与互评系统 — 设计稿公共库（design 阶段）
约定：本文件不写页面，只提供 shell / 组件 / 图表 / 代码块。
注意：所有 CSS 一律作为裸字符串拼接，绝不经过 % 格式化，避免 CSS 里的 % 被当成格式符。
"""
import os, re, html

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SYMS = re.findall(r'<symbol.*?</symbol>', open(os.path.join(BASE, 'icons.svg'), encoding='utf-8').read(), re.S)

TITLE = '编程课程作业提交与互评系统'
BRAND = '码课评'

# ---------------------------------------------------------------- 基础
def ic(name, size=18):
    return ('<svg class="ic" width="' + str(size) + '" height="' + str(size) + '" viewBox="0 0 24 24" fill="none" '
            'stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">'
            '<use href="#' + name + '"/></svg>')

def tag(text, cls='t-gray'):
    return '<span class="tag ' + cls + '">' + text + '</span>'

ST = {
    '草稿': 't-gray', '已发布': 't-blue', '提交中': 't-blue', '互评中': 't-amber',
    '评分中': 't-amber', '已归档': 't-green', '已撤回': 't-gray',
    '待登记': 't-gray', '已登记': 't-blue', '已进入互评': 't-amber', '已评分': 't-green',
    '待评': 't-amber', '已评': 't-green', '超时未评': 't-red', '已豁免': 't-gray',
    '待复核': 't-amber', '确认抄袭': 't-red', '误报': 't-green', '需面谈': 't-blue',
    '待受理': 't-amber', '处理中': 't-blue', '已驳回': 't-gray', '已改分': 't-green', '需重新互评': 't-amber',
    '未发布': 't-gray', '进行中': 't-blue', '已结课': 't-green',
    '高可信': 't-green', '中可信': 't-blue', '较低可信': 't-amber', '低可信': 't-red',
    '未运行': 't-gray', '通过': 't-green', '编译失败': 't-red',
    '按时': 't-green', '迟交': 't-amber',
}

def stag(s):
    return tag(s, ST.get(s, 't-gray'))

# ---------------------------------------------------------------- 角色与导航
NAV = {
 'teacher': [
   ('概览', [('i-dashboard', '教师工作台', '02-dash.html', ''), ('i-chart', '成绩统计', '29-stats.html', '')]),
   ('教学与作业', [('i-book', '课程管理', '03-course.html', ''),
               ('i-plus', '发布作业', '04-assignment-new.html', ''),
               ('i-list', '作业台账', '05-assignment-list.html', '8')]),
   ('提交与登记', [('i-terminal', '结果登记', '09-result-entry.html', '23'),
               ('i-upload', '批量导入', '10-result-import.html', '')]),
   ('互评与查重', [('i-shuffle', '互评分配', '11-peer-assign.html', ''),
               ('i-progress', '互评进度', '13-peer-progress.html', '6'),
               ('i-shield', '查重记录', '14-similarity-list.html', '4')]),
   ('成绩', [('i-score', '成绩审核', '16-score-review.html', ''),
            ('i-sum', '成绩汇总', '17-score-summary.html', ''),
            ('i-appeal', '申诉处理', '18-appeal.html', '3')]),
   ('规则', [('i-rule', '可信度规则', '20-credibility-rule.html', ''),
            ('i-trace', '计算留痕', '21-credibility-trace.html', '')]),
   ('个人', [('i-message', '站内消息', '25-messages.html', '5'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'student': [
   ('我的学习', [('i-home', '学生门户', '27-portal.html', ''),
              ('i-list', '作业列表', '05-assignment-list.html', ''),
              ('i-code', '提交代码', '07-submit.html', ''),
              ('i-file', '我的提交', '08-my-submissions.html', '')]),
   ('互评', [('i-shuffle', '待我评', '12-peer-desk.html', '3'), ('i-check', '我的互评', '13-peer-progress.html', '')]),
   ('成绩', [('i-sum', '我的成绩', '17-score-summary.html', ''), ('i-appeal', '成绩申诉', '18-appeal.html', '')]),
   ('个人', [('i-message', '站内消息', '25-messages.html', '2'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'ta': [
   ('概览', [('i-dashboard', '助教工作台', '02-dash.html', '')]),
   ('批改与登记', [('i-terminal', '结果登记', '09-result-entry.html', '12'),
               ('i-upload', '批量导入', '10-result-import.html', ''),
               ('i-progress', '互评进度', '13-peer-progress.html', '8')]),
   ('查重', [('i-shield', '查重记录', '14-similarity-list.html', '4'),
            ('i-eye', '相似度复核', '15-similarity-review.html', '')]),
   ('可信度', [('i-rule', '可信度明细', '19-credibility.html', ''),
             ('i-trace', '计算留痕', '21-credibility-trace.html', '')]),
   ('个人', [('i-message', '站内消息', '25-messages.html', '4'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'academic': [
   ('概览', [('i-dashboard', '教务看板', '02-dash.html', ''), ('i-chart', '成绩统计', '29-stats.html', '')]),
   ('教学运行', [('i-book', '课程管理', '03-course.html', ''), ('i-list', '作业台账', '05-assignment-list.html', '')]),
   ('成绩', [('i-score', '成绩审核', '16-score-review.html', ''),
            ('i-sum', '成绩汇总', '17-score-summary.html', ''),
            ('i-appeal', '申诉处理', '18-appeal.html', '5')]),
   ('系统', [('i-user', '用户管理', '22-users.html', ''), ('i-log', '操作日志', '24-system.html#log', '')]),
   ('个人', [('i-message', '站内消息', '25-messages.html', '3'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
 'sys': [
   ('系统设置', [('i-user', '用户管理', '22-users.html', ''),
              ('i-role', '角色权限', '23-roles.html', ''),
              ('i-menu', '菜单与字典', '24-system.html', ''),
              ('i-log', '操作日志', '24-system.html#log', '')]),
   ('概览', [('i-dashboard', '教师工作台', '02-dash.html', '')]),
   ('个人', [('i-message', '站内消息', '25-messages.html', '2'), ('i-user', '个人中心', '26-profile.html', '')]),
 ],
}
ROLE_USER = {
 'teacher':  ('陈立群', '任课教师 · 计算机学院', '陈'),
 'student':  ('林嘉禾', '软工2301 · 20230114', '林'),
 'ta':       ('周敏', '助教 · 数据结构', '周'),
 'academic': ('高文彬', '教务管理员 · 教学运行科', '高'),
 'sys':      ('系统管理员', '系统管理员', '管'),
}

# ---------------------------------------------------------------- 页面骨架
def _nav(role, crumb):
    out = []
    for group, items in NAV[role]:
        out.append('<div class="group">' + group + '</div>')
        for icon, label, href, badge in items:
            on = ' on' if (label in crumb) else ''
            b = ('<span class="badge">' + badge + '</span>') if badge else ''
            out.append('<a class="' + on.strip() + '" href="' + href + '">'
                       + ic(icon, 16) + '<span>' + label + '</span>' + b + '</a>')
    return ''.join(out)

def shell(role, title, crumb, body, head='', cls=''):
    name, rlabel, initial = ROLE_USER[role]
    doc = ['<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">',
           '<title>' + title + ' · ' + TITLE + '</title>',
           '<link rel="stylesheet" href="mock.css"><style>' + head + '</style></head>',
           '<body class="' + cls + '">',
           '<svg xmlns="http://www.w3.org/2000/svg" style="display:none">' + ''.join(SYMS) + '</svg>',
           '<div class="shell"><aside class="side">',
           '<div class="brand"><span class="mark">&lt;/&gt;</span><span>' + BRAND + '</span></div>',
           '<nav class="nav">' + _nav(role, crumb) + '</nav></aside>',
           '<div class="main"><header class="top"><div class="crumb">' + crumb + '</div><div class="spacer"></div>',
           '<span class="icon-btn">' + ic('i-search') + '</span>',
           '<span class="icon-btn" style="position:relative">' + ic('i-bell')
           + '<span class="badge" style="position:absolute;top:-6px;right:-6px;background:#C2703A;color:#fff;border-radius:9px;padding:0 5px;font-size:10px;line-height:15px">5</span></span>',
           '<span class="icon-btn">' + ic('i-help') + '</span>',
           '<div style="display:flex;align-items:center;gap:8px;font-size:13px">',
           '<span class="avatar">' + initial + '</span>',
           '<span><b style="font-weight:500">' + name + '</b>'
           '<span style="display:block;font-size:11px;color:#5B6770">' + rlabel + '</span></span></div>',
           '</header><main class="content">' + body + '</main></div></div></body></html>']
    return ''.join(doc)

def rawpage(title, body, head=''):
    return ('<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>' + title + ' · ' + TITLE
            + '</title><link rel="stylesheet" href="mock.css"><style>' + head + '</style></head><body>'
            + '<svg xmlns="http://www.w3.org/2000/svg" style="display:none">' + ''.join(SYMS) + '</svg>'
            + body + '</body></html>')

def page_head(title, sub, acts=''):
    return ('<div class="page-head"><div><h1>' + title + '</h1><div class="sub" style="margin-bottom:0">'
            + sub + '</div></div><div class="acts">' + acts + '</div></div>')

# ---------------------------------------------------------------- 组件
def kpis(items, cls='compact'):
    h = ['<div class="kpis ' + cls + '">']
    for it in items:
        label, val, unit = it[0], it[1], it[2]
        delta = it[3] if len(it) > 3 else ''
        dcls = it[4] if len(it) > 4 else ''
        gcls = it[5] if len(it) > 5 else ''
        d = ('<div class="delta ' + dcls + '">' + delta + '</div>') if delta else '<div class="delta" style="visibility:hidden">-</div>'
        h.append('<div class="kpi"><div class="label">' + label + '</div><div class="value ' + gcls + '">'
                 + val + '<small>' + unit + '</small></div>' + d + '</div>')
    h.append('</div>')
    return ''.join(h)

def card(title, body, sub='', cls='', bodycls=''):
    return ('<div class="c ' + cls + '"><div class="ct">' + title + '<span>' + sub
            + '</span></div><div class="cbody ' + bodycls + '">' + body + '</div></div>')

def table(head, rows, cls=''):
    h = ['<div class="tbl-wrap"><table class="' + cls + '"><thead><tr>']
    for c in head:
        h.append('<th>' + c + '</th>')
    h.append('</tr></thead><tbody>')
    for r in rows:
        h.append('<tr>' + ''.join('<td>' + str(c) + '</td>' for c in r) + '</tr>')
    h.append('</tbody></table></div>')
    return ''.join(h)

def pager(total, cur=1, size=10):
    pages = (total + size - 1) // size
    h = ['<div class="pager"><span>共 ' + str(total) + ' 条 · 每页 ' + str(size) + ' 条</span>']
    for p in range(1, min(pages, 5) + 1):
        h.append('<span class="pg ' + ('on' if p == cur else '') + '">' + str(p) + '</span>')
    h.append('<span class="pg">下一页</span></div>')
    return ''.join(h)

def toolbar(filters, btns=''):
    h = ['<div class="toolbar">']
    for f in filters:
        if f == 'input':
            h.append('<input class="search" placeholder="搜索编号 / 标题 / 学号">')
        elif f.startswith('sel:'):
            h.append('<select><option>' + f[4:] + '</option></select>')
        elif f.startswith('date:'):
            h.append('<input style="width:150px" value="' + f[5:] + '">')
        elif f.startswith('txt:'):
            h.append('<span style="font-size:13px;color:#5B6770">' + f[4:] + '</span>')
    h.append('<button class="btn ghost">查询</button><button class="btn">重置</button>'
             '<div style="flex:1"></div>' + btns + '</div>')
    return ''.join(h)

def kvrows(items, w=96):
    """窄卡片里的键值列表：左标签固定宽、右值自动换行，避免 .kv 在 300px 宽卡片里挤成两行"""
    h = ['<div style="font-size:12.5px">']
    for k, v in items:
        h.append('<div style="display:flex;gap:10px;padding:5px 0;border-bottom:1px dashed #E8EDEA">'
                 '<span style="flex:0 0 %dpx;color:#5B6770">' % w + k + '</span>'
                 '<span style="flex:1;color:#1F2933">' + v + '</span></div>')
    return ''.join(h) + '</div>'

def flow(cur, items=None):
    items = items or ['草稿', '已发布', '提交中', '互评中', '评分中', '已归档']
    idx = items.index(cur) if cur in items else -1
    h = ['<div class="flow">']
    for i, s in enumerate(items):
        c = 'done' if (idx >= 0 and i < idx) else ('on' if i == idx else '')
        h.append('<span class="st ' + c + '">' + s + '</span>')
        if i < len(items) - 1:
            h.append('<span class="arw">›</span>')
    h.append('</div>')
    return ''.join(h)

def timeline(events):
    h = ['<div class="timeline">']
    for st, title, who, when in events:
        h.append('<div class="ev ' + st + '"><div class="h">' + title + '</div><div class="d">'
                 + who + ' · ' + when + '</div></div>')
    return ''.join(h) + '</div>'

def note(text, kind='info'):
    color = {'info': '#1E4B3F', 'warn': '#C2703A', 'ok': '#2B7955', 'danger': '#B02A37'}[kind]
    return ('<div style="background:#F2F5F4;border:1px solid #DEE4E9;border-left:3px solid ' + color
            + ';padding:9px 12px;font-size:12px;color:#5B6770;border-radius:4px;line-height:1.75">' + text + '</div>')

# ---------------------------------------------------------------- 代码块
KW = set(('public static void int long double float boolean char String class new import package private '
          'protected final return if else for while do switch case break continue try catch finally throws '
          'throw extends implements interface abstract this super null true false def elif in not and or '
          'print lambda function let const var from as with async await').split())

def _hl(line):
    res, i, n = [], 0, len(line)
    while i < n:
        ch = line[i]
        if ch == '/' and i + 1 < n and line[i + 1] == '/':
            res.append('<span class="cm">' + html.escape(line[i:]) + '</span>'); break
        if ch == '#':
            res.append('<span class="cm">' + html.escape(line[i:]) + '</span>'); break
        if ch == '"' or ch == "'":
            j = i + 1
            while j < n and line[j] != ch:
                if line[j] == '\\':
                    j += 1
                j += 1
            res.append('<span class="str">' + html.escape(line[i:j + 1]) + '</span>')
            i = j + 1
            continue
        if ch.isalpha() or ch == '_':
            j = i
            while j < n and (line[j].isalnum() or line[j] == '_'):
                j += 1
            w = line[i:j]
            if w in KW:
                res.append('<span class="kw">' + w + '</span>')
            elif j < n and line[j] == '(':
                res.append('<span class="fn">' + w + '</span>')
            else:
                res.append(html.escape(w))
            i = j
            continue
        if ch.isdigit():
            j = i
            while j < n and (line[j].isdigit() or line[j] == '.'):
                j += 1
            res.append('<span class="num2">' + line[i:j] + '</span>')
            i = j
            continue
        res.append(html.escape(ch))
        i += 1
    return ''.join(res)

def code_block(lines, start=1, hl=(), h='h300', fname='', dim=(), marks=None):
    hh = ['<div class="code ' + h + '">']
    if fname:
        hh.append('<table><tbody>')
    else:
        hh.append('<table><tbody>')
    for i, ln in enumerate(lines):
        no = start + i
        cls = 'hl' if no in hl else ('dim' if no in dim else '')
        hh.append('<tr class="' + cls + '"><td class="ln">' + str(no) + '</td><td class="cl">'
                  + (_hl(ln) if ln.strip() else '&nbsp;') + '</td></tr>')
    hh.append('</tbody></table></div>')
    return ''.join(hh)

# ---------------------------------------------------------------- 图表
def chart_line(labels, series, h=180, w=560, ymax=None):
    pad_l, pad_b, pad_t = 34, 22, 8
    maxv = ymax or (max(max(s[2]) for s in series) * 1.15)
    iw, ih = w - pad_l - 6, h - pad_b - pad_t
    def X(i): return pad_l + iw * i / (len(labels) - 1)
    def Y(v): return pad_t + ih - ih * v / maxv
    g = []
    for k in range(5):
        gy = pad_t + ih * k / 4
        gv = maxv * (1 - k / 4)
        g.append('<line x1="' + str(pad_l) + '" y1="%.1f" x2="%d" y2="%.1f" stroke="#EDF0F3"/>' % (gy, w - 6, gy))
        g.append('<text x="' + str(pad_l - 6) + '" y="%.1f" font-size="10" fill="#8FA099" text-anchor="end">%.0f</text>' % (gy + 3, gv))
    for i, lb in enumerate(labels):
        if i == len(labels) - 1:
            g.append('<text x="' + str(w - 6) + '" y="' + str(h - 6) + '" font-size="10" fill="#8FA099" text-anchor="end">' + lb + '</text>')
        elif i % 2 == 0:
            g.append('<text x="%.1f" y="%d" font-size="10" fill="#8FA099" text-anchor="middle">%s</text>' % (X(i), h - 6, lb))
    for name, color, vals in series:
        pts = ' '.join('%.1f,%.1f' % (X(i), Y(v)) for i, v in enumerate(vals))
        g.append('<polyline points="' + pts + '" fill="none" stroke="' + color + '" stroke-width="2" stroke-linejoin="round"/>')
        for i, v in enumerate(vals):
            g.append('<circle cx="%.1f" cy="%.1f" r="2.4" fill="#fff" stroke="%s" stroke-width="1.6"/>' % (X(i), Y(v), color))
    leg = ''.join('<span style="display:inline-flex;align-items:center;gap:5px;margin-right:14px"><i style="width:9px;height:3px;border-radius:2px;background:'
                  + c + ';display:inline-block"></i>' + n + '</span>' for n, c, _ in series)
    return ('<div style="height:100%;display:flex;flex-direction:column"><div style="font-size:11px;color:#5B6770;margin-bottom:2px">'
            + leg + '</div><div style="flex:1;min-height:0"><svg class="chart-line" viewBox="0 0 ' + str(w) + ' ' + str(h)
            + '" preserveAspectRatio="none">' + ''.join(g) + '</svg></div></div>')

def chart_hbars(items, w=86):
    h = []
    mx = max(v for _, v, _ in items)
    for name, val, label in items:
        pct = max(3, val / mx * 100)
        h.append('<div class="hbar"><span class="n" style="width:' + str(w) + 'px;flex:0 0 ' + str(w) + 'px">'
                 + name + '</span><span class="t"><i style="width:%.1f%%"></i></span><span class="v">' % pct
                 + label + '</span></div>')
    return ''.join(h)

def chart_vbars(items, alt_from=4):
    h = ['<div class="vbars">']
    mx = max(v for _, v in items)
    for i, (lb, v) in enumerate(items):
        h.append('<div><span class="num">' + str(v) + '</span><span class="b '
                 + ('alt' if i >= alt_from else '') + '" style="height:%.1f%%"></span><span class="lb">' % (v / mx * 86)
                 + lb + '</span></div>')
    return ''.join(h) + '</div>'

def chart_donut(parts, mid_v, mid_l, size=126):
    r = size / 2 - 9
    c = 2 * 3.141592653589793 * r
    off, segs = 0.0, []
    for lab, pct, col in parts:
        d = c * pct / 100.0
        segs.append('<circle cx="%.1f" cy="%.1f" r="%.1f" fill="none" stroke="%s" stroke-width="13" '
                    'stroke-dasharray="%.2f %.2f" stroke-dashoffset="%.2f" transform="rotate(-90 %.1f %.1f)"/>'
                    % (size / 2, size / 2, r, col, d, c - d, -off, size / 2, size / 2))
        off += d
    leg = ''.join('<div><i style="background:' + c2 + ';display:inline-block"></i>' + l2
                  + '<b>' + str(p2) + '%</b></div>' for l2, p2, c2 in parts)
    return ('<div class="donut-wrap"><div style="position:relative;width:' + str(size) + 'px;height:' + str(size)
            + 'px;flex:0 0 ' + str(size) + 'px"><svg width="' + str(size) + '" height="' + str(size) + '">'
            + ''.join(segs) + '</svg><div style="position:absolute;inset:0;display:grid;place-items:center;text-align:center;line-height:1.2">'
            + '<div><b style="font-size:17px">' + mid_v + '</b><span style="font-size:11px;color:#5B6770;display:block">'
            + mid_l + '</span></div></div></div><div class="lg">' + leg + '</div></div>')

def chart_scatter(points, h=150, w=258, xlabel='互评分', ylabel='教师评分'):
    pad_l, pad_b, pad_t = 30, 18, 6
    iw, ih = w - pad_l - 8, h - pad_b - pad_t
    mx = max(p[1] for p in points) * 1.12
    my = max(p[2] for p in points) * 1.1
    def X(v): return pad_l + iw * v / mx
    def Y(v): return pad_t + ih - ih * v / my
    g = []
    for k in range(4):
        gy = pad_t + ih * k / 3
        g.append('<line x1="' + str(pad_l) + '" y1="%.1f" x2="%d" y2="%.1f" stroke="#EDF0F3"/>' % (gy, w - 8, gy))
        g.append('<text x="' + str(pad_l - 4) + '" y="%.1f" font-size="8" fill="#8FA099" text-anchor="end">%.0f</text>'
                 % (gy + 3, my * (1 - k / 3)))
    for k in range(4):
        gx = pad_l + iw * k / 3
        g.append('<text x="%.1f" y="%d" font-size="8" fill="#8FA099" text-anchor="middle">%.0f</text>' % (gx, h - 5, mx * k / 3))
    for lb, x, y in points:
        g.append('<circle cx="%.1f" cy="%.1f" r="3.4" fill="#3D7A66" fill-opacity=".68" stroke="#1E4B3F" stroke-width=".9"/>' % (X(x), Y(y)))
        if lb:
            g.append('<text x="%.1f" y="%.1f" font-size="7" fill="#5B6770" text-anchor="middle">%s</text>' % (X(x), Y(y) - 6, lb))
    return '<svg class="chart-line" viewBox="0 0 %d %d" preserveAspectRatio="none">%s</svg>' % (w, h, ''.join(g))

def fmt1(v):
    """保留一位小数的裸数字（不经过 % 格式化，避免与 CSS 的 % 冲突）"""
    return ('%.1f' % v)

def chart_group2(labels, s1, s2, n1='提交率', n2='迟交率', c1='#3D7A66', c2='#C2703A', unit='%'):
    """分组双柱：每类两根柱，用于提交率/迟交率对比"""
    mx = max(max(s1), max(s2)) * 1.16
    h = ['<div style="height:100%;display:flex;flex-direction:column">',
         '<div style="font-size:11px;color:#5B6770;margin-bottom:2px">',
         '<span style="display:inline-flex;align-items:center;gap:5px;margin-right:12px"><i style="width:9px;height:9px;border-radius:2px;background:'
         + c1 + ';display:inline-block"></i>' + n1 + '</span>',
         '<span style="display:inline-flex;align-items:center;gap:5px"><i style="width:9px;height:9px;border-radius:2px;background:'
         + c2 + ';display:inline-block"></i>' + n2 + '</span></div>',
         '<div style="flex:1;min-height:0;display:flex;align-items:flex-end;gap:14px;padding:0 2px 2px">']
    for i, lb in enumerate(labels):
        h1 = max(2, s1[i] / mx * 100)
        h2 = max(2, s2[i] / mx * 100)
        bar1 = '<div title="' + n1 + '" style="width:14px;height:' + fmt1(h1) + '%;background:' + c1 + ';border-radius:2px 2px 0 0"></div>'
        bar2 = '<div title="' + n2 + '" style="width:14px;height:' + fmt1(h2) + '%;background:' + c2 + ';border-radius:2px 2px 0 0"></div>'
        h.append('<div style="flex:1;min-width:0;height:100%;display:flex;flex-direction:column;justify-content:flex-end;align-items:center;gap:3px">'
                 '<div style="display:flex;align-items:flex-end;gap:4px;height:100%;width:100%;justify-content:center">'
                 + bar1 + bar2 +
                 '</div><span style="font-size:10px;color:#5B6770;white-space:nowrap">' + lb + '</span></div>')
    return ''.join(h) + '</div><div style="font-size:10px;color:#8FA099;text-align:right">单位 ' + unit + '</div></div>'

def chart_stack(rows):
    """rows: [(name, [(label,pct,color)...], right_text)]"""
    h = ['<div class="stack">']
    for name, parts, right in rows:
        segs = ''.join('<i style="width:' + ('%.1f' % p) + '%;background:' + c + '"></i>' for _, p, c in parts)
        h.append('<div class="srow"><span class="sname">' + name + '</span><span class="sbar">' + segs
                 + '</span><span class="sv">' + right + '</span></div>')
    return ''.join(h) + '</div>'

def score_rows(items):
    h = []
    for name, weight, score, note_ in items:
        col = '#B02A37' if score < 60 else ('#C2703A' if score < 75 else '#3D7A66')
        h.append('<div class="score-row"><span>' + name + ' <span style="color:#8FA099">' + weight + '</span></span>'
                 '<span class="bar"><i style="width:%.1f%%;background:%s"></i></span><span class="v">%.1f</span></div>'
                 % (score, col, score))
    return ''.join(h)
