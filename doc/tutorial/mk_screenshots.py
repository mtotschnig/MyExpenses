import sys

if (len(sys.argv) < 2):
  print "Usage: monkeyrunner mk_screenshots.py {lang}"
  sys.exit(0)

lang = sys.argv[1]
targetdir = '../../../MyExpenses.pages/tutorial_r2/' + lang + '/large/'

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

#introduction
snapshot('1')

##tutorial1 Managing accounts
#open "Edit Account" through backdoor
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
#close the virtual keyboard
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
#call our "backdoor that enters data"
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
snapshot('2')
#backdoor finishes
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep(1)

## tutorial2 Managing transactions
#open "New transaction" through backdoor
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
#close the virtual keyboard
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('3')
#call our "backdoor" that enters data amd finishes
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
#back at transaction list
#open context (we use the backdoor to make sure list is focused)
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('4')
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
snapshot('5')
#select Category import through backdoor
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
snapshot('6')
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
snapshot('7')
#select "Add new category through backdoor"
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
snapshot('8')
#Close dialog
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep(2)
#open context (we use the backdoor to make sure list is focused)
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('9')

#Tutorial 4 Export transactions
#back to main screen
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
#open "Reset" through backdoor
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep()
#confirm
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('10')

#Tutorial 5 Settings
sleep()
#open "Reset" through backdoor
device.press('KEYCODE_ENVELOPE', MonkeyDevice.DOWN)
sleep(3)
snapshot('11')

