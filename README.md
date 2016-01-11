For all your gitlab testing needs!
step 1) cd into Battlecode2016 and TYPE git pull
step 2) make your changes and move on ONLY WHEN THERE ARE NO BUGS IN YOUR CODE
step 3) TYPE git add [name of file you changed]
step 4) TYPE git commit -m "[insert descriptive message about what you did]"
step 5) TYPE git pull
step 6) TYPE git push 

Possible errors
step 5b) CONFLICT automatic merge failed //This means some doofus was editing
the same code you were editing. Now its YOUR job to fix it before others mess
it up even more!
step 5c) TYPE vi [name of file with errors]
step 5b) fix all the errors, it should label who made what changes
step 5c) GO to step 3)
step 5d) try TYPING git pull //if you did your job correctly it should be
error free now

step 4b) //You found yourself in some scary screen cuz u didnt add a message
step 4c)ESC :q!
step 4d) try step 4 again...

step 6)Rejected! Start at step 1 again :)