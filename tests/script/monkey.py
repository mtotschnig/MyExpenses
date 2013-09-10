import sys

if (len(sys.argv) < 3):
  print "Usage: monkeyrunner monkey.py {lang} {country}"
  sys.exit(0)

lang = sys.argv[1]
country = sys.argv[2]
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

def toOrigin():
  for _ in range(5):
    up()
    left()

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
device = MonkeyRunner.waitForConnection()

# start
package = 'org.totschnig.myexpenses'
activity = 'org.totschnig.myexpenses.activity.MyExpenses'
runComponent = package + '/' + activity
extraDic = {} 
extraDic['instrument_language'] = lang 
extraDic['instrument_country'] = country 
device.startActivity(extras=extraDic,component=runComponent)

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
toOrigin()
down()
enter()
snapshot("grouped_list")

#4 NewFromTemplate
menu(1)
snapshot("new_from_template")

#5 ExportAndReset
back()
menu(2)
snapshot("export")

#6 SelectCategory
back()
toOrigin()
sleep()
right()
#don't know why one is enough here
right()
sleep()
enter()
toOrigin()
for _ in range(5):
  down()
enter()
down()
enter()
snapshot("categories")

#7 Calculator
back()
up()
up()
right()
right()
enter()
snapshot("calculator")

#8 Backup
back()
back()
menu(5)
if lang == 'de':
  distance = 17
else:
  distance = 16
for _ in range(distance):
  down()
enter()
snapshot("backup")

#9 Light Theme
back()
back()
menu(5)
for _ in range(5):
  down()
enter()
down()
enter()
back()
snapshot("light_theme")

