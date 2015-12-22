#this script currently is designed for being run on Nexus S
import sys
import os

from monkey_funcs import *

if len(sys.argv) < 3:
  print "Usage: monkeyrunner monkey.py {lang} {stage}"
  sys.exit(0)

lang = sys.argv[1]
if lang == 'ar':
	monkey_funcs.RTL = True
stage = sys.argv[2]
targetdir = '/Users/privat/MyExpenses/doc/screenshots/phone/' + lang + '/'
package = 'org.totschnig.myexpenses'

if not os.path.isdir(targetdir):
  print "Make sure directory %s exists" % targetdir
  sys.exit(0)

init(targetdir)

def main():
  #1 ManageAccounts
  toTopStart()
  enter()
  sleep()
  down()
  down()
  down()
  sleep()
  toTopStart(8)
  snapshot("manage_accounts")
  
  #3 GrooupedList
  back()
  snapshot("grouped_list")
  
  #4 Templates and Plans
  activity = 'org.totschnig.myexpenses.activity.ManageTemplates'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  sleep()
  toTopStart()
  down()
  end()
  enter() #plans tab
  sleep(3)
  down()
  enter()
  # openinstances
  # since we are using windowActionModeOverlay, currently we are not able to focus cab through keyboard navigation
  # down()
  # device.press('DPAD_CENTER', MonkeyDevice.DOWN) # open CAB
  # sleep()
  # up()
  # up()
  # up()
  # left()
  # enter() #apply instance
  # sleep(4)
  snapshot("plans")
  
  #5 ExportAndReset
  back()
  menu(1)
  snapshot("export")
  
  #6 Calculator
  back()
  # hit FAB if we are lucky (run on an S4)
  device.touch(1000,1800,MonkeyDevice.DOWN_AND_UP)
  activity = 'org.totschnig.myexpenses.activity.CalculatorInput'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  snapshot("calculator")
  
  #7 Split
  back()
  back()
  toTopStart(10)
  down()#split is first, currently no transaction created from plan
  enter()
  sleep(2)
  end()
  enter()
  #give time for loading
  sleep(2)
  back()#close virtual keyboard
  snapshot("split")
  
  #8 Attach picture
  back()
  toTopStart()
  down()
  down() #currently second in list
  enter()
  sleep(2)
  end()
  enter()
  #give time for loading
  sleep(2)
  back()#close virtual keyboard
  #navigate to imageView
  toTopStart()
  down()
  down()
  down()
  down()
  down()
  down()
  end()
  enter()
  snapshot("attach_picture")
  
  #8 Distribution
  back()
  back()
  menu(0)
  toTopStart()
  end()
  enter()
  down()
  down()
  down()
  enter()
  sleep()
  snapshot("distribution")
  
  #11 Help
  back()
  activity = 'org.totschnig.myexpenses.activity.Help'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  snapshot("help")
  
  #9 Backup
  back()
  activity = 'org.totschnig.myexpenses.activity.BackupRestoreActivity'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent,action="myexpenses.intent.backup")
  snapshot("backup")
  
  #10 Password
  back()
  sleep()
  activity = 'org.totschnig.myexpenses.activity.MyPreferenceActivity'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent,extras={'openPrefKey' : 'screen_protection'})
  sleep()
  up()
  sleep(1)
  down()
  sleep(1)
  enter()
  sleep(1)
  device.press('KEYCODE_DPAD_CENTER')
  sleep(1)
  snapshot("password")
  
  if (stage == "1"):
    finalize()
    return
  
  #10 Light Theme
  back()
  back()
  back()
  device.startActivity(component=runComponent,extras={'openPrefKey' : 'pref_ui_theme'}) 
  down()
  down()
  enter()
  back()
  back()
  snapshot("light_theme")
  
  finalize()

main()
