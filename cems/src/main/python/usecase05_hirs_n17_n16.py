from workflow import Workflow

w = Workflow('usecase05_hirs_n17_n16', 7, '/group_workspaces/cems2/fiduceo/Software/mms/config')
w.add_primary_sensor('hirs-n17', '2002-07-10', '2013-04-09', '1.0')
w.add_secondary_sensor('hirs-n16', '2002-07-10', '2013-04-09', '1.0')

w.set_usecase_config('usecase-05.xml')

w.run_matchup(hosts=[('localhost', 24)])