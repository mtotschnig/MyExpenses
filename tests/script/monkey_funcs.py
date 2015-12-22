from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice

device = MonkeyRunner.waitForConnection()
BACKDOOR_KEY = 'KEYCODE_CAMERA'
#for arab screenshots set to true
RTL = False
global targetdir

def init(_targetdir):
  global targetdir
  targetdir = _targetdir
  package = 'org.totschnig.myexpenses'
  activity = 'org.totschnig.myexpenses.activity.MyExpenses'
  runComponent = package + '/' + activity
  extra = {'_id': "3"}
  device.startActivity(component=runComponent,extras=extra)

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

def end():
  if RTL:
    left()
  else:
    right()

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

def toTopStart(force=5):
  if RTL:
    toTopRight(force)
  else:
  	toTopLeft(force)

def toTopLeft(force=5):
  for _ in range(force*2):
    up()
  for _ in range(force):
    left()
    
def toTopRight(force=5):
  for _ in range(force*2):
    up()
  for _ in range(force):
    right()

def toBottomLeft(force=5):
  for _ in range(force*2):
    down()
  for _ in range(force):
    left()

def finalize():
  back()
  back()
  back()