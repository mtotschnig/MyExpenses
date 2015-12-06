#this script is designed for being run on 7 or 10 inch tablet
import sys
import os

from monkey_funcs import *

if len(sys.argv) < 3:
  print "Usage: monkeyrunner monkey.py {lang} {stage}"
  sys.exit(0)

lang = sys.argv[1]
stage = sys.argv[2]
targetdir = '/Users/privat/MyExpenses/doc/screenshots/tablet-7/' + lang + '/'
package = 'org.totschnig.myexpenses'

if not os.path.isdir(targetdir):
  print "Make sure directory %s exists" % targetdir
  sys.exit(0)

init(targetdir)

def main():
  sleep()
  toTopLeft()
  snapshot("main")
  
  activity = 'org.totschnig.myexpenses.activity.ManageCategories'
  runComponent = package + '/' + activity
  extra = {'account_id': "3"}
  device.startActivity(component=runComponent,action="myexpenses.intent.distribution",extras=extra)
  sleep()
  toTopLeft()
  right()
  enter()
  down()
  down()
  down()
  enter()
  sleep()
  snapshot("distribution")
  
  back()
  
  #give time for loading
  sleep(3)
  activity = 'org.totschnig.myexpenses.activity.ExpenseEdit'
  runComponent = package + '/' + activity
  extra = {'_id': "1"}
  device.startActivity(component=runComponent,extras=extra)
  sleep(3)
  back()#close virtual keyboard
  snapshot("edit")
  
  finalize()

main()
