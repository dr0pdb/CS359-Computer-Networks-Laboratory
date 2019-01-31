The format of the config.txt file.

1. It should be present in the directory where the program is located.
2. The first line of the file should contain a positive integer denoting the maximum number of connections in the server.
3. The second line should contain the blocked IPs separated by a single space.
4. The third and the last line should contain the full path of the default file returned by the server.


How to test blocked ip feature?
Please add your local ip (/0:0:0:0:0:0:0:1) in the config file and try to access it from the browser. You'll see the message "Request from a blocked ip" in the terminal.