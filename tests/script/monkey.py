import sys

if (len(sys.argv) < 3):
  print "Usage: monkeyrunner monkey.py {lang} {country}"
  sys.exit(0)

lang = sys.argv[1]
country = sys.argv[2]
stage = sys.argv[3]
targetdir = '/home/michael/programmieren/MyExpenses/doc/screenshots/neu/' + lang + '/'
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

  #2 AggregateDialog
  right()
  right()
  enter()
  down()
  down()
  snapshot("aggregate_dialog")

  #3 GrooupedList
  back()
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
  #hit Templats button
  enter()
  down()
  enter()
  sleep(2)
  right()
  right()
  right()
  enter()
  sleep(2)
  for _ in range(10):
    down()
  enter()
  sleep(2)
  right()
  enter()
  sleep(5)
  snapshot("plans")

  #5 ExportAndReset
  back()
  back()
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
    distance = 20
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
    distance = 25
  else:
    distance = 24
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


