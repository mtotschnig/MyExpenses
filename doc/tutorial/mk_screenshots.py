import sys

if (len(sys.argv) < 2):
  print "Usage: monkeyrunner mk_screenshots.py {lang}"
  sys.exit(0)

lang = sys.argv[1]
targetdir = '../../../MyExpenses.pages/tutorial/' + lang + '/large/'

def snapshot(number):
  filename = 'step'+number+'.png'
  print filename
  result = device.takeSnapshot()
  result.writeToFile(targetdir + filename,'png')

def sleep(duration=1):
  MonkeyRunner.sleep(duration)
  print "sleeping"
  

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
device = MonkeyRunner.waitForConnection()
#package = 'org.totschnig.myexpenses'
#activity = 'org.totschnig.myexpenses.MyExpenses'
#runComponent = package + '/' + activity
#device.startActivity(component=runComponent)

##tutorial1 Managing accounts
#open menu
device.press('KEYCODE_MENU', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('1')
#triggers "Select Account" from menu
device.press('KEYCODE_E', MonkeyDevice.DOWN)
sleep()
#open context (we use the backdoor to make sure list is focused)
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('2')
#open "Edit Account"
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
#close the virtual keyboard
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
#call our "backdoor that enters data"
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
snapshot('3')
#backdoor finishes
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
MonkeyRunner.sleep(1)
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()

## tutorial2 Managing transactions
device.press('KEYCODE_MENU', MonkeyDevice.DOWN_AND_UP)
sleep()
#open "Add Transaction"
device.press('KEYCODE_A', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('5')
#call our "backdoor that enters data"
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
#open "ContextMenu"
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('6')
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()

##tutorial3 Managing categories
#open transaction edit
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
#navigate to Category select button
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
#trigger it
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
#show menu
device.press('KEYCODE_MENU', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('7')
#select Category import
device.press('KEYCODE_B', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('8')
#select import source based on lang
if (lang == 'it'):
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
if (lang == 'de'):
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
if (lang == 'fr'):
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
#execute import and give it some time
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep(30)
snapshot('9')
#show menu
device.press('KEYCODE_MENU', MonkeyDevice.DOWN_AND_UP)
sleep()
#select "Add new category"
device.press('KEYCODE_A', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('10')
#Close dialog
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep(2)
#open context (we use the backdoor to make sure list is focused)
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('11')

#Tutorial 4 Export transactions
#back to main screen
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
#open menu
device.press('KEYCODE_MENU', MonkeyDevice.DOWN_AND_UP)
sleep()
#call "Reset" and call
device.press('KEYCODE_C', MonkeyDevice.DOWN)
sleep()
#confirm
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('12')

