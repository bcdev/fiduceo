from workflow import Workflow

w = Workflow('usecase05_hirs_n18_n15', 7, '/group_workspaces/cems2/fiduceo/Software/mms/config')
w.add_primary_sensor('hirs-n18', '2006-01-01', '2016-06-03', '1.0')
w.add_secondary_sensor('hirs-n15', '2006-01-01', '2016-06-03', '1.0')

w.set_usecase_config('usecase-05.xml')

w.run_matchup(hosts=[('localhost', 24)])