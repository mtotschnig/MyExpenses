import sys

if (len(sys.argv) < 3):
  print "Usage: monkeyrunner monkey.py {lang} {stage}"
  sys.exit(0)

lang = sys.argv[1]
stage = sys.argv[2]
targetdir = '/home/michael/programmieren/MyExpenses/doc/screenshots/neu/' + lang + '/'
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

def down_and_up(key):
  device.press(key,MonkeyDevice.DOWN_AND_UP)

def back():
  down_and_up('KEYCODE_BACK')
  sleep()

def down():
  down_and_up('KEYCODE_DPAD_DOWN')

def right():
  down_and_up('KEYCODE_DPAD_RIGHT')

def left():
  down_and_up('KEYCODE_DPAD_LEFT')

#select the nth item from menu (0 indexed)
def menu(n):
  down_and_up('KEYCODE_MENU')
  sleep()
  for _ in range(10):
    up() #make sure we start from top
  for _ in range(n):
    down()
  enter()

def enter():
  down_and_up('KEYCODE_ENTER')
  sleep()

def up():
  down_and_up('KEYCODE_DPAD_UP')

def toTopLeft():
  for _ in range(10):
    up()
  for _ in range(5):
    left()

def toBottomLeft():
  for _ in range(10):
    down()
  for _ in range(5):
    left()

def finalize():
  back()
  back()
  back()

def main():
  #1 ManageAccounts
  left()
  left()
  enter()
  snapshot("manage_accounts")
  
  #3 GrooupedList
  toTopLeft()
  down()
  enter()
  snapshot("grouped_list")
  
  if (stage == "1"):
    finalize()
    return
  
  #4 Templates and Plans
  toBottomLeft()
  right()
  right()
  right()
  enter() #temmplates button
  toTopLeft()
  down()
  right()
  enter()
  sleep(2)
  down()
  enter()
  sleep(2)
  down()#apply first instance
  device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
  device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
  device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
  sleep()
  enter()
  sleep()
  down()#cancel second instance
  down()
  device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
  device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
  device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
  sleep()
  down()
  down()
  enter()
  sleep()
  snapshot("plans")
  
  #5 ExportAndReset
  back()
  menu(0)
  snapshot("export")
  
  #6 Calculator
  back()
  toBottomLeft()
  sleep()
  right()
  sleep()
  enter()
  toTopLeft()
  activity = 'org.totschnig.myexpenses.activity.CalculatorInput'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  snapshot("calculator")
  
  #7 Split
  back()
  back()
  toTopLeft()
  down()
  down()#split is second, first is the transaction created from plan
  enter()
  right()
  enter()
  
  #give time for loading
  sleep(2)
  snapshot("split")
  
  #8 Distribution
  back()
  menu(3)
  right()
  enter()
  down()
  down()
  down()
  enter()
  down()
  enter()
  snapshot("distribution")
  
  #9 Backup
  back()
  menu(4)
  if lang == 'de':
    distance = 19
  elif lang == 'zh':
    distance = 17
  else:
    distance = 18
  for _ in range(distance):
    down()
  enter()
  snapshot("backup")
  
  #10 Password
  back()
  back()
  menu(4)
  if lang == 'de':
    distance = 26
  elif lang == 'zh':
    distance = 24
  else:
    distance = 25
  for _ in range(distance):
    down()
  enter()
  enter()
  snapshot("password")
  
  #10 Light Theme
  back()
  back()
  menu(4)
  for _ in range(5):
    down()
  enter()
  down()
  enter()
  back()
  snapshot("light_theme")
  
  #11 Help
  menu(5)
  snapshot("help")
  finalize()

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
device = MonkeyRunner.waitForConnection()

# start
package = 'org.totschnig.myexpenses'
activity = 'org.totschnig.myexpenses.activity.MyExpenses'
runComponent = package + '/' + activity
device.startActivity(component=runComponent)
main()


