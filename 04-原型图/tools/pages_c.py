#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面 20-29：规则配置 / 计算留痕 / 用户 / 角色权限 / 菜单字典 / 消息 / 个人中心 / 学生门户 / 移动端 / 成绩统计"""
from base import *


# ============================================================ 20 规则配置
def p20():
    rows = [
        ('v2.1', '评分偏离度 25 / 评语长度 20 / 完成率 20 / 相似度 20 / 一致性 15', '2026-10-20 09:00', '陈立群', '<span class="tag t-green">生效中</span>'),
    ]
    body = [page_head('互评可信度规则配置',
                      '权重合计必须等于 100% · 修改后记录版本号与生效时间 · 历史计算不受影响',
                      '<button class="btn">恢复默认</button><button class="btn primary">保存并生效（生成 v2.2）</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:12px">')
    body.append('<div class="card"><h2 style="display:flex;justify-content:space-between">当前权重'
                '<span style="font-size:12px;font-weight:400;color:#2B7955">合计 100% ✓</span></h2>'
                '<div class="slider-row"><span class="nm">评分偏离度<small>与教师评分均值的偏差</small></span>'
                '<span class="track"><i style="width:25%"></i><b style="left:calc(25% - 8px)"></b></span>'
                '<span class="vv">25<small>%</small></span></div>'
                '<div class="slider-row"><span class="nm">评语有效长度<small>去套话字数 + 是否引用行号</small></span>'
                '<span class="track"><i style="width:20%"></i><b style="left:calc(20% - 8px)"></b></span>'
                '<span class="vv">20<small>%</small></span></div>'
                '<div class="slider-row"><span class="nm">互评完成率<small>近 3 次任务按期完成比例</small></span>'
                '<span class="track"><i style="width:20%"></i><b style="left:calc(20% - 8px)"></b></span>'
                '<span class="vv">20<small>%</small></span></div>'
                '<div class="slider-row"><span class="nm">提交相似度<small>同作业最高相似度</small></span>'
                '<span class="track"><i style="width:20%"></i><b style="left:calc(20% - 8px)"></b></span>'
                '<span class="vv">20<small>%</small></span></div>'
                '<div class="slider-row"><span class="nm">评分一致性<small>同提交多份互评标准差</small></span>'
                '<span class="track"><i style="width:15%"></i><b style="left:calc(15% - 8px)"></b></span>'
                '<span class="vv">15<small>%</small></span></div></div>')
    body.append('<div class="card"><h2>等级阈值</h2>'
                '<table><thead><tr><th>等级</th><th>分数区间</th><th>对成绩的影响</th></tr></thead><tbody>'
                '<tr><td><span class="tag t-green">高可信</span></td><td>≥ 85</td><td>互评分按权重直接计入</td></tr>'
                '<tr><td><span class="tag t-blue">中可信</span></td><td>65 – 84</td><td>互评分 ×0.8 系数折算，明细中说明</td></tr>'
                '<tr><td><span class="tag t-red">低可信</span></td><td>&lt; 45</td><td>不计入成绩，需追加互评或助教复核</td></tr>'
                '</tbody></table></div>')
    body.append('<div class="card"><h2>版本记录</h2>'
                + table(['版本', '权重组合', '生效时间', '修改人', '状态'], rows) + '</div>')
    body.append('</div>')
    body.append('<div style="width:330px;flex:0 0 330px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>修改影响预演</h2>'
                + note('若把「评语有效长度」从 20% 提到 25%：<br>'
                       '高可信 268 → 251 条（−17）<br>低可信 9 → 12 条（+3）<br>'
                       '受影响作业：作业3、作业4<br><br>仅影响新计算或主动重算的数据，历史结果保留 v2.1 口径。', 'warn') + '</div>'
                '<div class="card"><h2>等级分布（v2.1 实测）</h2>'
                + chart_vbars([('高可信', 268), ('中可信', 121), ('较低', 34), ('低可信', 9)], 2) + '</div>'
                '<div class="card"><h2>规则说明</h2>'
                '<div style="font-size:12px;color:#5B6770;line-height:2">'
                '· 相似度总分 = 结构 0.5 + 标识符 0.3 + 注释 0.2<br>'
                '· 无查重记录时该项不计分，权重按剩余项归一<br>'
                '· 仅一次互评时「评分一致性」不参与计算</div></div></div>')
    body.append('</div>')
    return shell('teacher', '可信度规则配置', '规则 / <b>可信度规则配置</b>', ''.join(body),
                 '.slider-row{margin-bottom:7px}.slider-row .nm small{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}')


# ============================================================ 21 计算留痕
def p21():
    rows = [
        ('<span class="mono">CT261102000412</span>', '林嘉禾 / 作业3', 'v2.1', '96.0 / 100.0 / 92.0 / 78.0 / 94.0', '92.5', '<span class="tag t-green">高可信</span>', '11-02 06:40'),
        ('<span class="mono">CT261102000411</span>', '王雨桐 / 作业3', 'v2.1', '88.0 / 100.0 / 100.0 / 62.0 / 90.0', '88.1', '<span class="tag t-green">高可信</span>', '11-02 06:40'),
        ('<span class="mono">CT261102000410</span>', '陈思远 / 作业3', 'v2.1', '72.0 / 84.0 / 67.0 / 71.0 / 60.0', '71.6', '<span class="tag t-blue">中可信</span>', '11-02 06:40'),
        ('<span class="mono">CT261102000409</span>', '孙悦 / 作业3', 'v2.1', '68.0 / 76.0 / 53.0 / 80.0 / 72.0', '69.0', '<span class="tag t-blue">中可信</span>', '11-02 06:40'),
        ('<span class="mono">CT261102000408</span>', '郑凯 / 作业3', 'v2.1', '— / 42.0 / 0.0 / 88.0 / —', '38.4', '<span class="tag t-red">低可信</span>', '11-02 06:40'),
        ('<span class="mono">CT261101000233</span>', '周子豪 / 作业2', 'v2.0', '90.0 / 100.0 / 100.0 / 84.0 / 88.0', '92.4', '<span class="tag t-green">高可信</span>', '11-01 21:12'),
    ]
    body = [page_head('计算留痕', '每次计算保存输入快照、规则版本、各因素得分、总分、等级与生成时间，可复算且不受后续数据变化影响',
                      '<button class="btn">按作业筛选</button><button class="btn ghost">' + ic('i-export', 14) + ' 导出快照</button>'
                      '<button class="btn primary">重算所选</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0">'
                + table(['快照编号', '学生 / 作业', '规则版本', '五项得分（偏离/长度/完成/相似/一致）', '可信度分', '等级', '生成时间'],
                        [r + ('',) for r in rows])
                + pager(412)
                + '<div class="card" style="margin-top:14px"><h2>快照详情 · CT261102000412</h2>'
                + '<div class="cmp" style="gap:16px">'
                '<div><div style="font-size:12px;color:#5B6770;margin-bottom:6px">输入快照（JSON，计算时冻结）</div>'
                + code_block(['{', '  "task_id": 1073,', '  "rule_version": "v2.1",',
                              '  "deviation": {"peer": 86.0, "teacher_mean": 87.4, "diff": 1.4},',
                              '  "comment": {"chars": 61, "line_refs": 3, "cliche": false},',
                              '  "finish_rate": {"recent": 3, "done": 3},',
                              '  "similarity": {"max": 73.1, "pair": "#A-1017"},',
                              '  "consistency": {"reviews": 3, "std": 4.2},',
                              '  "weights": {"dev": 25, "len": 20, "rate": 20, "sim": 20, "cons": 15}',
                              '}'], 1, (4, 9), 'h200') + '</div>'
                '<div><div style="font-size:12px;color:#5B6770;margin-bottom:6px">计算过程（逐项可复算）</div>'
                + table(['因素', '公式', '结果'], [
                    ('评分偏离度', '(1 − 1.4 / 12) × 100', '96.0'),
                    ('评语有效长度', '61 ≥ 40 且引用 3 处行号', '100.0'),
                    ('互评完成率', '(3/3) × 92 基准', '92.0'),
                    ('提交相似度', '(100 − 73.1) × 2.9', '78.0'),
                    ('评分一致性', '(1 − 4.2 / 12) × 100', '94.0'),
                    ('加权合计', '24.0 + 20.0 + 18.4 + 15.6 + 14.1', '<b>92.5</b>'),
                ]) + '</div></div>'
                '<div style="margin-top:12px">' + note('同一输入 + 同一规则版本必然得到同一结果；'
                                                      '修改权重只影响新计算或主动重算的数据。', 'ok') + '</div></div></div>')
    body.append('<div class="card" style="width:320px;flex:0 0 320px">'
                '<h2>本次重算影响</h2>'
                + chart_hbars([('高可信', 268, '268'), ('中可信', 121, '121'), ('较低可信', 34, '34'), ('低可信', 9, '9')], 72)
                + '<div style="border-top:1px solid #DEE4E9;margin-top:10px;padding-top:10px;font-size:12px;color:#5B6770;line-height:2">'
                '已生成快照 412 条<br>覆盖作业 6 个 · 学生 86 人<br>人工修正 7 条（保留原始结果）<br>'
                '最近重算：11-02 06:40（v2.1）</div></div>')
    body.append('</div>')
    return shell('teacher', '计算留痕', '规则 / <b>计算留痕</b>', ''.join(body))


# ============================================================ 22 用户管理
def p22():
    rows = [
        ('<span class="mono">teacher01</span>', '陈立群', '任课教师', '计算机学院', '139****2201', '正常'),
        ('<span class="mono">teacher02</span>', '李慧珊', '任课教师', '计算机学院', '139****2202', '正常'),
        ('<span class="mono">ta01</span>', '周敏', '助教', '计算机学院', '139****3310', '正常'),
        ('<span class="mono">ta02</span>', '吴迪', '助教', '软件学院', '139****3311', '停用'),
        ('<span class="mono">20230114</span>', '林嘉禾', '学生', '软工2301', '139****5614', '正常'),
        ('<span class="mono">20230115</span>', '王雨桐', '学生', '软工2301', '139****5615', '正常'),
        ('<span class="mono">academic01</span>', '高文彬', '教务管理员', '教务处', '139****9001', '正常'),
        ('<span class="mono">admin</span>', '系统管理员', '系统管理员', '信息中心', '139****9000', '正常'),
    ]
    body = [page_head('用户管理', '共 128 个账号 · 教师 12 · 助教 8 · 学生 105 · 教务 2 · 管理员 1',
                      '<button class="btn ghost">' + ic('i-upload', 14) + ' 批量导入</button>'
                      '<button class="btn primary">' + ic('i-plus', 14) + ' 新增用户</button>')]
    body.append(toolbar(['input', 'sel:全部角色', 'sel:全部学院', 'sel:全部状态'], '<button class="btn sm">重置密码</button>'))
    body.append('<div class="card" style="padding:0">'
                + table(['账号', '姓名', '角色', '所属学院/班级', '手机号', '状态', '操作'], [
                    (r[0], r[1], '<span class="tag t-blue">' + r[2] + '</span>', r[3], r[4],
                     stag('进行中') if r[5] == '正常' else stag('已撤回'),
                     '<a href="#">编辑</a> · <a href="#">分配角色</a> · <a href="#">重置密码</a>')
                    for r in rows]) + '</div>')
    body.append(pager(128, 1, 10))
    return shell('sys', '用户管理', '系统设置 / <b>用户管理</b>', ''.join(body))


# ============================================================ 23 角色权限
def p23():
    menus = [('概览', ['教师工作台', '成绩统计']),
             ('教学与作业', ['课程管理', '发布作业', '作业台账', '作业详情']),
             ('提交与登记', ['结果登记', '批量导入']),
             ('互评与查重', ['互评分配', '互评进度', '查重记录', '相似度复核']),
             ('成绩', ['成绩审核', '成绩汇总', '申诉处理']),
             ('规则', ['可信度规则配置', '计算留痕'])]
    tree = ['<div style="font-size:13px">']
    for g, items in menus:
        tree.append('<div style="padding:6px 0;border-bottom:1px dashed #E8EDEA">'
                    '<label style="display:flex;align-items:center;gap:8px;font-weight:500">'
                    '<input type="checkbox" checked style="height:auto"> ' + g + '</label>'
                    '<div style="padding:6px 0 2px 24px;display:flex;flex-wrap:wrap;gap:8px 18px">')
        for it in items:
            tree.append('<label style="display:flex;align-items:center;gap:6px;font-size:12.5px;color:#5B6770;font-weight:400">'
                        '<input type="checkbox" checked style="height:auto"> ' + it + '</label>')
        tree.append('</div></div>')
    tree.append('</div>')
    body = [page_head('角色权限', '菜单权限 + 按钮权限 + 数据范围三层控制 · 前端隐藏按钮不等于授权，写接口后端复核',
                      '<button class="btn">复制角色</button><button class="btn primary">保存权限</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="width:300px;flex:0 0 300px">'
                '<div class="card" style="padding:0"><div style="padding:12px 14px;border-bottom:1px solid #DEE4E9;font-size:13px;font-weight:500">'
                '角色列表</div>'
                '<div style="padding:8px 10px">'
                '<div class="qitem on"><span class="l">任课教师<span class="tag t-gray">12 人</span></span><span>全部</span></div>'
                '<div class="qitem"><span class="l">助教<span class="tag t-gray">8 人</span></span><span>本部门</span></div>'
                '<div class="qitem"><span class="l">学生<span class="tag t-gray">105 人</span></span><span>本人</span></div>'
                '<div class="qitem"><span class="l">教务管理员<span class="tag t-gray">2 人</span></span><span>全部</span></div>'
                '<div class="qitem"><span class="l">系统管理员<span class="tag t-gray">1 人</span></span><span>全部</span></div>'
                '</div></div>'
                '<div class="card" style="margin-top:12px"><h2>数据范围</h2>'
                '<select style="width:100%;margin-bottom:8px"><option>全部数据</option><option>本部门</option><option>本人</option></select>'
                '<div style="font-size:12px;color:#5B6770;line-height:1.9">学生角色只看本人提交与成绩；'
                '助教看本部门课程；教师看本人授课班级。</div></div></div>')
    body.append('<div style="flex:1;min-width:0">'
                '<div class="card"><h2 style="display:flex;justify-content:space-between">任课教师 · 菜单权限'
                '<span style="font-size:12px;font-weight:400;color:#5B6770">已选 22 / 25</span></h2>'
                + ''.join(tree) + '</div>'
                '<div class="card" style="margin-top:12px"><h2>按钮权限（示例：作业台账）</h2>'
                '<div style="display:flex;gap:18px;flex-wrap:wrap;font-size:13px">'
                '<label><input type="checkbox" checked style="height:auto"> 发布作业</label>'
                '<label><input type="checkbox" checked style="height:auto"> 分配互评</label>'
                '<label><input type="checkbox" checked style="height:auto"> 登记结果</label>'
                '<label><input type="checkbox" checked style="height:auto"> 开始查重</label>'
                '<label><input type="checkbox" style="height:auto"> 删除作业</label>'
                '<label><input type="checkbox" style="height:auto"> 归档</label></div>'
                '<div style="margin-top:12px">' + note('「菜单管理」与「分配菜单」两项对所有角色隐藏，'
                                                      '仅在系统管理员的基础配置入口可达。', 'info') + '</div></div></div>')
    body.append('</div>')
    return shell('sys', '角色权限', '系统设置 / <b>角色权限</b>', ''.join(body))


# ============================================================ 24 菜单与字典
def p24():
    body = [page_head('菜单与基础数据', '菜单三级权限 · 业务字典统一取值 · 关键写操作留痕',
                      '<button class="btn">刷新缓存</button><button class="btn primary">' + ic('i-plus', 14) + ' 新增</button>')]
    body.append('<div class="tabs"><a class="on" href="#">菜单管理<span class="c">25</span></a>'
                '<a href="#dict">字典管理<span class="c">9</span></a><a href="#">参数配置<span class="c">6</span></a>'
                '<a href="#log" id="log">操作日志<span class="c">1 284</span></a></div>')
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="flex:1;min-width:0">'
                + table(['菜单名称', '类型', '路由', '权限标识', '排序', '可见', '操作'], [
                    ('教学与作业', '目录', '—', '—', '2', '是', '<a href="#">编辑</a> · <a href="#">新增子项</a>'),
                    ('　发布作业', '菜单', '/assignment/new', 'assignment:add', '2-2', '是', '<a href="#">编辑</a>'),
                    ('　作业台账', '菜单', '/assignment/list', 'assignment:list', '2-3', '是', '<a href="#">编辑</a>'),
                    ('　　· 发布/撤回按钮', '按钮', '—', 'assignment:publish/withdraw', '2-3-1', '—', '<a href="#">编辑</a>'),
                    ('互评与查重', '目录', '—', '—', '4', '是', '<a href="#">编辑</a>'),
                    ('　互评分配', '菜单', '/peer/assign', 'peer:assign', '4-1', '是', '<a href="#">编辑</a>'),
                    ('　查重记录', '菜单', '/similarity/list', 'similarity:list', '4-3', '是', '<a href="#">编辑</a>'),
                ])
                + '<div class="card" style="margin-top:12px" id="log"><h2>操作日志（最近 8 条）</h2>'
                + table(['时间', '操作人', '模块', '操作', '业务编号', 'IP'], [
                    ('11-02 06:40', '陈立群', '可信度', '重算 412 条', '—', '10.20.3.18'),
                    ('11-02 06:12', '林嘉禾', '申诉', '提交申诉', 'SS2611020003', '10.20.7.44'),
                    ('11-02 00:10', '系统', '查重', '批量检测 84 份', 'CD-B2601102-07', '127.0.0.1'),
                    ('11-01 22:30', '王雨桐', '申诉', '提交申诉', 'SS2611010007', '10.20.7.19'),
                    ('11-01 20:02', '陈立群', '互评', '确认分配 252 条', 'ZY261019002', '10.20.3.18'),
                ]) + '</div>'
                + '</div>')
    body.append('<div style="width:340px;flex:0 0 340px;display:flex;flex-direction:column;gap:12px">'
                '<div class="card" id="dict"><h2>业务字典</h2>'
                + table(['字典类型', '项数', '示例'], [
                    ('assignment_status', '7', '草稿/已发布/提交中…'), ('submission_status', '4', '待登记/已登记…'),
                    ('review_status', '4', '待评/已评/超时未评'), ('similarity_result', '4', '待复核/确认抄袭…'),
                    ('appeal_type', '3', '互评分偏低/查重误判/计算错误'), ('appeal_status', '5', '待受理/处理中…'),
                    ('language_type', '6', 'Java/Python/C++/C/JS/Go'), ('match_type', '4', '变量重命名/结构一致…'),
                ]) + '</div>'
                '<div class="card"><h2>参数配置</h2>'
                '<div class="kv" style="grid-template-columns:104px 1fr;font-size:12.5px">'
                '<div style="color:#5B6770;text-align:right">单文件上限</div><div>200 KB</div>'
                '<div style="color:#5B6770;text-align:right">代码行数上限</div><div>2 000 行</div>'
                '<div style="color:#5B6770;text-align:right">迟交窗口上限</div><div>7 天</div>'
                '<div style="color:#5B6770;text-align:right">相似度告警线</div><div>70%</div>'
                '<div style="color:#5B6770;text-align:right">评语最少字数</div><div>20 字</div>'
                '<div style="color:#5B6770;text-align:right">申诉有效期</div><div>7 天</div>'
                '<div style="color:#5B6770;text-align:right">归档条件</div><div>发布满 7 天且无待处理申诉</div>'
                '</div></div></div>')
    body.append('</div>')
    return shell('sys', '菜单与基础数据', '系统设置 / <b>菜单与字典</b>', ''.join(body))


# ============================================================ 25 站内消息
def p25():
    msgs = [
        ('1', '互评任务已分配：作业3 还有 2 份待评', '截止 2026-11-09 23:59，逾期将影响可信度分。', '10 分钟前', True, '待办'),
        ('2', '作业4 已进入提交中，截止 11-09 23:59', '你已提交 2 次，最后一次用例 5/6。', '1 小时前', True, '作业'),
        ('3', '查重记录 CD2611020031 需你复核', '相似度 82.40%，命中「变量重命名 + 结构一致」。', '2 小时前', True, '查重'),
        ('4', '成绩已发布：作业2 得分 91.2', '互评可信度 高可信，可在成绩汇总查看明细。', '昨天 20:12', False, '成绩'),
        ('5', '你的申诉 SS2611020003 已受理', '由李慧珊老师处理，预计 2 个工作日内答复。', '昨天 18:40', False, '申诉'),
        ('6', '评语质量提示：第 2 份互评评语不足 20 字', '已提示补充，若再次提交将标记低质量。', '10-31 21:04', False, '提醒'),
    ]
    body = [page_head('站内消息', '未读 3 条 · 共 26 条', '<button class="btn">全部标为已读</button><button class="btn ghost">消息设置</button>')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    left = ['<div style="flex:1;min-width:0"><div class="card" style="padding:0">']
    for i, (no, t, d, tm, unread, kind) in enumerate(msgs):
        left.append('<div style="display:flex;gap:12px;padding:13px 16px;border-bottom:1px solid #EDF0F3;align-items:flex-start;'
                    + ('background:#F7FBF9;box-shadow:inset 3px 0 0 #1E4B3F' if unread else '') + '">'
                    '<div style="width:30px;height:30px;border-radius:8px;background:#E7F0EC;color:#1E4B3F;display:grid;place-items:center;flex:0 0 30px">'
                    + ic('i-message', 16) + '</div>'
                    '<div style="flex:1;min-width:0"><div style="font-size:13px;' + ('font-weight:600' if unread else '') + '">'
                    + t + '</div><div style="font-size:12px;color:#5B6770;margin-top:2px;line-height:1.7">' + d + '</div></div>'
                    '<div style="text-align:right;flex:0 0 88px"><div style="font-size:11px;color:#5B6770">' + tm + '</div>'
                    '<span class="tag t-gray" style="margin-top:4px">' + kind + '</span></div></div>')
    left.append('</div>' + pager(26) + '</div>')
    body.append(''.join(left))
    body.append('<div style="width:300px;flex:0 0 300px">'
                '<div class="card"><h2>消息类型</h2>'
                '<div style="font-size:13px;line-height:2.4">'
                '<div><span class="tag t-amber">待办</span> <span style="float:right;color:#5B6770">8 条</span></div>'
                '<div><span class="tag t-blue">作业</span> <span style="float:right;color:#5B6770">6 条</span></div>'
                '<div><span class="tag t-blue">查重</span> <span style="float:right;color:#5B6770">4 条</span></div>'
                '<div><span class="tag t-green">成绩</span> <span style="float:right;color:#5B6770">5 条</span></div>'
                '<div><span class="tag t-gray">申诉</span> <span style="float:right;color:#5B6770">3 条</span></div></div></div>'
                '<div class="card" style="margin-top:12px"><h2>提醒规则</h2>'
                + note('互评截止前 2 小时未完成的评阅人自动收到提醒；作业截止前 24 小时未提交的学生收到催交通知；'
                       '查重待复核超过 24 小时提醒助教。', 'info') + '</div></div>')
    body.append('</div>')
    return shell('student', '站内消息', '个人 / <b>站内消息</b>', ''.join(body))


# ============================================================ 26 个人中心
def p26():
    body = [page_head('个人中心', '资料修改、密码修改与头像上传，头像上传后右上角同步显示', '')]
    body.append('<div class="row" style="flex-wrap:nowrap;align-items:flex-start">')
    body.append('<div style="width:300px;flex:0 0 300px"><div class="card" style="text-align:center;padding:24px">'
                '<div style="width:84px;height:84px;border-radius:50%;background:#5E9C86;color:#fff;display:grid;place-items:center;'
                'font-size:30px;font-weight:600;margin:0 auto 12px">陈</div>'
                '<div style="font-size:16px;font-weight:600">陈立群</div>'
                '<div style="font-size:12px;color:#5B6770;margin-top:4px">任课教师 · 计算机学院</div>'
                '<div style="margin-top:14px"><button class="btn sm">更换头像</button></div>'
                '<div style="border-top:1px solid #DEE4E9;margin-top:16px;padding-top:14px;font-size:12.5px;color:#5B6770;line-height:2.2;text-align:left">'
                '<div>账号 <span style="float:right;color:#1F2933" class="mono">teacher01</span></div>'
                '<div>工号 <span style="float:right;color:#1F2933" class="mono">T2018033</span></div>'
                '<div>授课班级 <span style="float:right;color:#1F2933">软工2301 / 2302</span></div>'
                '<div>最近登录 <span style="float:right;color:#1F2933">11-02 06:30</span></div></div></div></div>')
    body.append('<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:12px">'
                '<div class="card"><h2>基本资料</h2><div class="fgrid">'
                '<div class="it"><span>姓名</span><input value="陈立群"></div>'
                '<div class="it"><span>手机号</span><input value="139 2201 8830"></div>'
                '<div class="it"><span>邮箱</span><input value="chenlq@univ.edu.cn"></div>'
                '<div class="it"><span>办公室</span><input value="信息楼 A-512"></div>'
                '<div class="it full"><span>研究方向</span><input value="软件工程 · 程序分析与代码质量"></div></div>'
                '<div style="margin-top:14px"><button class="btn primary">保存资料</button></div></div>'
                '<div class="card"><h2>修改密码</h2><div class="fgrid">'
                '<div class="it"><span>原密码</span><input type="password" value="······"></div>'
                '<div class="it"><span>新密码</span><input type="password" value="········"></div>'
                '<div class="it"><span>确认密码</span><input type="password" value="········"></div></div>'
                '<div style="margin-top:14px"><button class="btn">更新密码</button>'
                '<span style="font-size:12px;color:#5B6770;margin-left:12px">密码 8–20 位，需含大小写与数字</span></div></div>'
                '<div class="card"><h2>我的通知设置</h2>'
                '<div style="display:flex;gap:26px;flex-wrap:wrap;font-size:13px">'
                '<label><input type="checkbox" checked style="height:auto"> 互评待办提醒</label>'
                '<label><input type="checkbox" checked style="height:auto"> 查重复核提醒</label>'
                '<label><input type="checkbox" checked style="height:auto"> 申诉处理提醒</label>'
                '<label><input type="checkbox" style="height:auto"> 每周汇总邮件</label></div></div></div>')
    body.append('</div>')
    return shell('teacher', '个人中心', '个人 / <b>个人中心</b>', ''.join(body))


# ============================================================ 27 学生门户
def p27():
    cards = [
        ('作业3：多线程售票模拟', 'Java程序设计 · 截止 11-02 23:59', '已提交 v1 · 用例 5/5', '待互评'),
        ('作业4：文件IO与异常处理', 'Java程序设计 · 截止 11-09 23:59', '已提交 v2 · 用例 5/6', '已登记'),
        ('实验3：二叉树遍历', '数据结构与算法 · 截止 11-06 23:59', '未提交', '进行中'),
    ]
    body = ['<div class="portal"><div class="pnav">'
            '<span class="brand"><span style="width:24px;height:24px;border-radius:6px;background:#1E4B3F;color:#fff;'
            'display:grid;place-items:center;font-family:var(--mono);font-size:11px">&lt;/&gt;</span>' + BRAND + '</span>'
            '<span class="lk on">我的作业</span><span class="lk">互评</span><span class="lk">成绩</span>'
            '<span class="lk">消息</span><div class="spacer"></div>'
            '<span style="font-size:13px;color:#5B6770">林嘉禾 · 软工2301</span>'
            '<span class="avatar">林</span></div><div class="pbody">'
            '<div class="hero"><div><h2>晚上好，林嘉禾</h2>'
            '<p>作业3 今晚 23:59 截止，你已提交 v1（用例 5/5）；还有 2 份互评待完成，11-09 截止。</p></div>'
            '<button class="btn">去提交作业</button></div>']
    body.append('<div class="grid" style="grid-template-columns:2fr 1fr;margin-top:18px">')
    body.append('<div><div class="pcard"><h3>进行中的作业</h3>')
    for t, sub, st, tagt in cards:
        body.append('<div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0;border-bottom:1px solid #EDF0F3">'
                    '<div><div style="font-size:14px;font-weight:500">' + t + '</div>'
                    '<div style="font-size:12px;color:#5B6770;margin-top:3px">' + sub + '</div>'
                    '<div style="font-size:12px;color:#5B6770;margin-top:3px">' + st + '</div></div>'
                    '<div style="text-align:right"><span class="tag ' + ('t-green' if tagt == '已登记' else ('t-amber' if tagt == '待互评' else 't-blue')) + '">'
                    + tagt + '</span><div style="margin-top:8px"><button class="btn sm">查看</button></div></div></div>')
    body.append('<div style="padding-top:12px;font-size:13px"><a href="05-assignment-list.html">查看全部作业 →</a></div></div>')
    body.append('<div class="pcard" style="margin-top:14px"><h3>我的成绩趋势</h3>'
                + chart_line(['作业1', '作业2', '作业3'], [('我的成绩', '#3D7A66', [78.6, 91.2, 87.4]),
                                                         ('班级均分', '#C2703A', [80.2, 82.4, 78.4])], 150, 560)
                + '</div></div>')
    body.append('<div><div class="pcard"><h3>待我互评</h3>'
                '<div style="font-size:13px;color:#5B6770;line-height:2">还有 <b style="color:#C2703A">2 份</b> 待评，11-09 23:59 截止</div>'
                '<div style="margin-top:10px"><button class="btn primary" style="width:100%">进入互评工作台</button></div></div>'
                '<div class="pcard" style="margin-top:14px"><h3>最近消息</h3>'
                '<div style="font-size:12.5px;color:#5B6770;line-height:1.9">'
                '<div style="padding:7px 0;border-bottom:1px solid #EDF0F3">互评任务已分配：还有 2 份待评</div>'
                '<div style="padding:7px 0;border-bottom:1px solid #EDF0F3">作业4 已进入提交中</div>'
                '<div style="padding:7px 0">成绩已发布：作业2 得分 91.2</div></div></div>'
                '<div class="pcard" style="margin-top:14px"><h3>本学期概况</h3>'
                '<div class="kv" style="grid-template-columns:84px 1fr;font-size:12.5px">'
                '<div style="color:#5B6770;text-align:right">已提交</div><div>4 / 4</div>'
                '<div style="color:#5B6770;text-align:right">迟交</div><div>1 次</div>'
                '<div style="color:#5B6770;text-align:right">平均分</div><div>85.7</div>'
                '<div style="color:#5B6770;text-align:right">班级排名</div><div>第 9 / 42</div></div></div></div>')
    body.append('</div></div></div>')
    return rawpage('学生门户', ''.join(body))


# ============================================================ 28 移动端
def p28():
    def phone(title, inner, tab=0):
        tabs = ['作业', '互评', '成绩', '我的']
        t = ''.join('<span class="' + ('on' if i == tab else '') + '">' + x + '</span>' for i, x in enumerate(tabs))
        return ('<div class="phone"><div class="st"><span>9:41</span><span>5G ▮▮▮ 86%</span></div>'
                '<div class="hd2">' + title + '</div><div class="bd">' + inner + '</div>'
                '<div class="bt">' + t + '</div></div>')
    a = ('<div class="mcard" style="background:#1E4B3F;color:#fff;border:0">'
         '<div style="font-size:12px;color:#BFD5CC">作业3 · 今晚 23:59 截止</div>'
         '<div style="font-size:15px;font-weight:600;margin:4px 0 6px">多线程售票模拟</div>'
         '<div style="font-size:12px;color:#D8E7E1">已提交 v1 · 用例 5/5 · 待互评</div>'
         '<button class="btn sm" style="margin-top:10px;background:rgba(255,255,255,.16);border-color:rgba(255,255,255,.3);color:#fff">查看提交</button></div>'
         '<div class="mcard"><div class="t1"><span>作业4：文件IO与异常处理</span><span class="tag t-green">已提交</span></div>'
         '<div class="t2">截止 11-09 23:59 · 用例 5/6 · v2</div></div>'
         '<div class="mcard"><div class="t1"><span>实验3：二叉树遍历</span><span class="tag t-amber">未提交</span></div>'
         '<div class="t2">数据结构与算法 · 截止 11-06 23:59</div></div>'
         '<div class="mcard"><div class="t1"><span>大作业：响应式企业站</span><span class="tag t-blue">进行中</span></div>'
         '<div class="t2">Web前端开发 · 截止 11-20 23:59</div></div>')
    b = ('<div class="mcard"><div class="t1"><span>匿名提交 #A-1073</span><span class="tag t-amber">待评</span></div>'
         '<div class="t2">作业3 · TicketSeller.java · 13 行</div>'
         '<div style="margin-top:8px;background:#F3F6F5;border-radius:6px;padding:8px;font-family:var(--mono);font-size:10.5px;line-height:1.7;color:#233A31">'
         'public void sell() {<br>&nbsp;&nbsp;synchronized (this) {<br>&nbsp;&nbsp;&nbsp;&nbsp;if (ticket &gt; 0) {</div></div>'
         '<div class="mcard"><div style="font-size:12px;color:#5B6770;margin-bottom:8px">正确性 47 / 60</div>'
         '<div style="height:6px;background:#EDF0F4;border-radius:3px"><i style="display:block;height:100%;width:78%;background:#3D7A66;border-radius:3px"></i></div>'
         '<div style="font-size:12px;color:#5B6770;margin:10px 0 8px">可读性 18 / 25</div>'
         '<div style="height:6px;background:#EDF0F4;border-radius:3px"><i style="display:block;height:100%;width:72%;background:#3D7A66;border-radius:3px"></i></div>'
         '<div style="font-size:12px;color:#5B6770;margin:10px 0 8px">代码规范 11 / 15</div>'
         '<div style="height:6px;background:#EDF0F4;border-radius:3px"><i style="display:block;height:100%;width:73%;background:#3D7A66;border-radius:3px"></i></div>'
         '<div style="display:flex;justify-content:space-between;margin-top:12px;font-size:13px"><span style="color:#5B6770">总分</span>'
         '<b style="font-size:20px">76.0</b></div></div>'
         '<button class="btn primary" style="width:100%;height:44px">提交互评</button>')
    body = ['<div style="background:#EDF1EF;min-height:100vh;padding:26px">'
            '<div style="text-align:center;font-size:13px;color:#5B6770;margin-bottom:18px">'
            '移动端（<b style="color:#1F2933">390 × 780</b>）· 小于 768px 时侧栏改抽屉、表格改信息卡片、主按钮固定安全区</div>'
            '<div style="display:flex;gap:40px;justify-content:center;flex-wrap:wrap">'
            + phone('我的作业', a, 0) + phone('互评工作台', b, 1) + '</div></div>']
    return rawpage('移动端', ''.join(body))


# ============================================================ 29 成绩统计
def p29():
    body = [page_head('成绩统计', '按题目、班级、分数段三维统计 · 统计口径与明细一致 · 最近导出：成绩单_软工2301_作业3.xlsx（陈立群 11-02 07:20）',
                      '<select style="width:150px"><option>2026学年第一学期</option></select>'
                      '<button class="btn ghost">' + ic('i-export', 14) + ' 导出</button>')]
    body.append(kpis([('课程数', '6', '门', '', '', ''), ('学生数', '249', '人', '', '', ''),
                      ('总评平均分', '78.4', '分', '↑ 2.1 环比', '', ''), ('及格率', '91.9', '%', '', '', ''),
                      ('优秀率', '18.6', '%', '', '', ''), ('互评完成率', '82.4', '%', '', 'down', '')], 'compact'))
    body.append('<div class="dash">')
    body.append(card('按题目平均分与标准差', chart_group2(
        ['作业1', '作业2', '作业3', '作业4', '作业5', '作业6'],
        [86.2, 82.4, 78.4, 74.1, 71.6, 68.9], [8.4, 9.1, 11.2, 12.6, 13.4, 14.1],
        '平均分', '标准差', '#3D7A66', '#C2703A', '分'), '6 次作业', 'span2'))
    body.append(card('分数段占比', chart_donut(
        [('90+', 19, '#1E4B3F'), ('80-89', 30, '#3D7A66'), ('70-79', 27, '#C2703A'),
         ('60-69', 16, '#A9B7B0'), ('<60', 8, '#B02A37')], '78.4', '平均分', 118), '249 人'))
    body.append(card('各班平均分', chart_hbars(
        [('软工2301', 78.4, '78.4'), ('软工2302', 80.1, '80.1'), ('软工2202', 75.6, '75.6'),
         ('计科2302', 77.2, '77.2'), ('计科2401', 72.8, '72.8'), ('软工2101', 84.3, '84.3')], 68), '6 个教学班'))
    body.append(card('各分数段人数分布', chart_vbars(
        [('<60', 20), ('60-69', 40), ('70-79', 67), ('80-89', 75), ('90+', 47)], 3), '按最终成绩'))
    body.append(card('互评可信度等级占比', chart_stack([
        ('软工2301', [('高', 62, '#1E4B3F'), ('中', 28, '#3D7A66'), ('较低', 8, '#C2703A'), ('低', 2, '#B02A37')], '62%'),
        ('软工2302', [('高', 58, '#1E4B3F'), ('中', 30, '#3D7A66'), ('较低', 9, '#3C7A66'), ('低', 3, '#B02A37')], '58%'),
        ('软工2202', [('高', 47, '#1E4B3F'), ('中', 33, '#3D7A66'), ('较低', 14, '#C2703A'), ('低', 6, '#B02A37')], '47%'),
        ('计科2302', [('高', 41, '#1E4B3F'), ('中', 34, '#3D7A66'), ('较低', 17, '#C2703A'), ('低', 8, '#B02A37')], '41%'),
    ]), '按班级', 'span2'))
    body.append(card('成绩与互评完成率关系', chart_scatter(
        [('', 72, 68), ('', 88, 82), ('', 91, 90), ('', 64, 61), ('', 79, 80), ('', 95, 93),
         ('', 58, 55), ('', 84, 86), ('', 67, 70), ('', 90, 88), ('', 74, 72), ('', 81, 79)], 150, 264),
        '12 个班级'))
    body.append('</div>')
    return shell('academic', '成绩统计', '概览 / <b>成绩统计</b>', ''.join(body),
                 '.dash{grid-auto-rows:305px}')


PAGES = [('20-credibility-rule', p20), ('21-credibility-trace', p21), ('22-users', p22),
         ('23-roles', p23), ('24-system', p24), ('25-messages', p25), ('26-profile', p26),
         ('27-portal', p27), ('28-mobile', p28), ('29-stats', p29)]
