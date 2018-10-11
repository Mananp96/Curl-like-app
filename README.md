## Curl-like-app

A http Client application(works as curl command) which connects to servers and get the Response from Server.

## Command to Run:
```
javac httpc.java Client.java
java httpc [Command]
e.g java httpc -v get http://httpbin.org/get?course=maths^&assignment=1
```
## Detailed Usage

httpc is a curl-like application but supports HTTP protocol only.

### Usage:

```
httpc help
httpc command [arguments]
```

The commands are:
- get executes a HTTP GET request and prints the response.
- post executes a HTTP POST request and prints the response.
- help prints this screen.

### Get Usage

```
httpc help get
httpc get [-v] [-h key:value] URL
```

- Get executes a HTTP GET request for a given URL.
- -v Prints the detail of the response such as protocol, status, and headers.
- -h key:value Associates headers to HTTP Request with the format 'key:value'.

### Post Usage

```
httpc help post
httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL
```

Post executes a HTTP POST request for a given URL with inline data or from file.

- -v Prints the detail of the response such as protocol, status, and headers.
- -h key:value Associates headers to HTTP Request with the format 'key:value'.
- -d string Associates an inline data to the body HTTP POST request.
- -f file Associates the content of a file to the body HTTP POST request.

Either [-d] or [-f] can be used but not both.
