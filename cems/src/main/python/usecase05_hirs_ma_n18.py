from workflow import Workflow

w = Workflow('usecase05_hirs_ma_n18', 7, '/group_workspaces/cems2/fiduceo/Software/mms/config')
w.add_primary_sensor('hirs-ma', '2006-11-21', '2016-06-03', '1.0')
w.add_secondary_sensor('hirs-n18', '2006-11-21', '2016-06-03', '1.0')

w.set_usecase_config('usecase-05.xml')

w.run_matchup(hosts=[('localhost', 24)])