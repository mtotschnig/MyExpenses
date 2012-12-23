import sys

if (len(sys.argv) < 2):
  print "Usage: monkeyrunner mk_screenshots.py {lang}"
  sys.exit(0)

lang = sys.argv[1]
targetdir = '../../../MyExpenses.pages/' + lang + '/tutorial_r4/large/'
BACKDOOR_KEY = 'KEYCODE_CAMERA'

def snapshot(title):
  filename = title+'.png'
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
snapshot('introduction_mainscreen')

##tutorial1 Managing accounts
#open "Edit Account" through backdoor
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
#close the virtual keyboard
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
#call our "backdoor that enters data"
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
snapshot('accounts_createnew')
#backdoor finishes
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep(1)

## tutorial2 Managing transactions
#open "New transaction" through backdoor
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
#close the virtual keyboard
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep(3)
snapshot('transactions_add')
#call our "backdoor" that enters data
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
#navigate to Payment method select button
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('transactions_pickmethod')
#close the dialog and call "backdoor" to finish activity
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
#back at transaction list
#open context (we use the backdoor to make sure list is focused)
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('transactions_contextmenu')
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
snapshot('categories_beforeimport')
#select Category import through backdoor
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
snapshot('settings_import_picksource')
#select import source based on lang
if (lang != 'en'):
  device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
  if (lang != 'fr'):
    device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
    if (lang != 'de'):
      device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
      if (lang != 'it'):
        device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)

device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
#navigate down to buttons, we should arrive at the middle button
#"Categories"
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
#execute import and give it some time
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep(20)
snapshot('categories_afterimport')
#select "Add new category through backdoor"
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
snapshot('categories_createnew')
#Close dialog
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep(2)
#open context (we use the backdoor to make sure list is focused)
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN)
sleep()
snapshot('categories_contextmenu')

#Tutorial 4 Export transactions
#back to main screen
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
#open "Reset" through backdoor
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep()
#confirm
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('export_result')

#Tutorial 5 Settings
sleep()
#open "MyPreferenceActivity through backdoor
device.press(BACKDOOR_KEY, MonkeyDevice.DOWN)
sleep(3)
snapshot('settings_mainscreen')
sleep()
#navigate to "Manage payment methods"
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
#enter "Edit Payment Method"
device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
sleep()
device.press('KEYCODE_ENTER', MonkeyDevice.DOWN_AND_UP)
sleep()
#close the virtual keyboard
#device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
sleep()
snapshot('settings_manage_method_edit')

