# CompSys725-Assignment1
 
## Instructions

### Setup of server and client
1. Clone repo to preferred directory
2. Navigate to /assignment1/server and open a terminal
3. Run ```java server.java```
4. Navgate to /assignment1/client and open a terminal
5. Run ```java client.java```

Client sending messages across will be written in code blocks <br>
&ensp;&ensp;&ensp;&ensp;Server response will be written with indentation
```
Client message to server
    Server response

```

### Logging In
There are 4 accounts stored on the server side *users.txt* file.
| User | Account | Password |
| ----------- | ----------- | ---|
| admin |       |       |
| user1 | acct1 | pass1 |
| foo   | bar   | baz   |
| michael | jackson      | heehee      |

#### ```USER <user-id>```
---
Checks whether the user is in the database and responds accordingly <br>
The admin user bypasses the check for acct and password

Example user "Foo"
```
USER foo
    +User-id valid, send account and password
``` 
```
USER admin
    +admin logged in
``` 
```
USER pineapple
    -Invalid user-id, try again
``` 

#### ```ACCT <acct-id>```
---
```
ACCT bar
    +Account valid, send password
```
```
ACCT spongebob
    -Invalid account, try again
```

#### ```PASS <password>```
---
```
PASS baz
    ! logged in
```
If ```PASS``` is called before ```ACCT```
```
PASS baz
    +Send account
```
```
PASS squarepants   
    -Wrong password,try again
```

#### ```TYPE { A | B | C }```
---
The mapping of the stored file to the transmission byte stream is controlled by the type. The default is binary

```
TYPE A
    +Using Ascii mode
```

#### ```LIST { F | V } <directory-path>```
---
Lists the files in the selected directory. If no directory is given, lists the files in the current directory.

```
LIST f
    Name                   Date modified           Size            Owner
    1/ 
    Needles.png
    server.java
    TCPserver.class
    test/
    users.txt
```
```
LIST v
    Name                    Date modified           Size            Owner
    1/                      28/08/2022 13:15:26        -            wljas
    Needles.png             28/08/2022 13:15:26   178810            wljas
    server.java             28/08/2022 13:15:26    19173            wljas
    TCPserver.class         28/08/2022 13:15:26    13012            wljas
    test/                   28/08/2022 13:15:26        -            wljas
    users.txt               28/08/2022 13:15:26       65            wljas
```

#### ```CDIR <new-directory>```
---
Changes working directory to the specified directory on the remote host
>Not fully implemented

#### ```KILL <file-spec>```
---
Deletes the specified file from the remote host
```
KILL notreal.txt
    -File does not exist
```
```
KILL actualfile.txt
    +File deleted
```

#### ```NAME <old-file-spec>```
---
Renames the specified file

```
NAME notreal.txt
    -File does not exist
```
```
NAME name.txt
    +File exists, send new name
TOBE name1.txt
    +File renamed
```

#### ```DONE```
---
Tells the system you are done
```
DONE
    +closing connection
```

#### ```RETR <file-spec>```
---
Requests the remote system to send a specified file
```
RETR notreal.png
    -File does not exist
```
```
RETR Needles.png
    File size: 178810
SEND
    Save file as: 
newpic.png
    Saved to /<user>
```
```
RETR Needles.png
    File size: 178810
STOP
    +ok, RETR aborted
```

#### ```STOR { NEW | OLD | APP } <file-spec>```
---
Tells the remote system to recieve the following file <br>
Not fully implemented
