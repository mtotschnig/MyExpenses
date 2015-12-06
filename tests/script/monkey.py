#this script currently is designed for being run on Nexus S
import sys
import os

if len(sys.argv) < 3:
  print "Usage: monkeyrunner monkey.py {lang} {stage}"
  sys.exit(0)

lang = sys.argv[1]
stage = sys.argv[2]
targetdir = '/Users/privat/MyExpenses/doc/screenshots/neu/' + lang + '/'
if not os.path.isdir(targetdir):
  print "Make sure directory %s exists" % targetdir
  sys.exit(0)

BACKDOOR_KEY = 'KEYCODE_CAMERA'
def snapshot(title):
  sleep()
  filename = title+'.png'
  print filename
  result = device.takeSnapshot()
  result.writeToFile(targetdir + filename,'png')

def sleep(duration=1):
  MonkeyRunner.sleep(duration)
  print "sleeping"

def back():
  device.press('KEYCODE_BACK')
  sleep()

def down():
  device.press('KEYCODE_DPAD_DOWN')

def right():
  device.press('KEYCODE_DPAD_RIGHT')

def left():
  device.press('KEYCODE_DPAD_LEFT')

#select the nth item from menu (0 indexed)
def menu(n):
  device.press('KEYCODE_MENU')
  sleep()
  for _ in range(10):
    up() #make sure we start from top
  for _ in range(n):
    down()
  enter()

def enter():
  device.press('KEYCODE_ENTER')
  sleep()

def up():
  device.press('KEYCODE_DPAD_UP')

def toTopLeft(force=5):
  for _ in range(force*2):
    up()
  for _ in range(force):
    left()

def toBottomLeft(force=5):
  for _ in range(force*2):
    down()
  for _ in range(force):
    left()

def finalize():
  back()
  back()
  back()

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
device = MonkeyRunner.waitForConnection()

# start
package = 'org.totschnig.myexpenses'
activity = 'org.totschnig.myexpenses.activity.MyExpenses'
runComponent = package + '/' + activity
#we start with the third account set up in fixture
extra = {'_id': "3"}
device.startActivity(component=runComponent,extras=extra)

def main():

  #1 ManageAccounts
  toTopLeft()
  enter()
  sleep()
  down()
  down()
  down()
  sleep()
  toTopLeft(8)
  snapshot("manage_accounts")

  #3 GrooupedList
  back()
  snapshot("grouped_list")
    
  #4 Templates and Plans
  activity = 'org.totschnig.myexpenses.activity.ManageTemplates'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  sleep()
  toTopLeft()
  down()
  right()
  enter() #plans tab
  sleep(3)
  down()
  enter() #openinstances
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
  toTopLeft(10)
  down()#split is first, currently no transaction created from plan
  enter()
  sleep(2)
  right()
  enter()
  #give time for loading
  sleep(2)
  back()#close virtual keyboard
  snapshot("split")
  
  #8 Attach picture
  back()
  toTopLeft()
  down()
  down() #currently second in list
  enter()
  sleep(2)
  right()
  enter()
  #give time for loading
  sleep(2)
  back()#close virtual keyboard
  #navigate to imageView
  toTopLeft()
  down()
  down()
  down()
  down()
  down()
  down()
  right()
  enter()
  snapshot("attach_picture")
  
  #8 Distribution
  back()
  back()
  menu(0)
  toTopLeft()
  right()
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
  down()
  enter()
  sleep(2)
  device.press('KEYCODE_DPAD_CENTER')
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
