<style type="text/css">
h1,h2,h3 { font-family: 'Ubuntu', Arial, sans-serif; }
h2 { margin-top: 2em; }
figure img { margin-top: 5px; margin-bottom: 5px }
figcaption { padding-top: 5px; }
.break { page-break-before: always; }
</style>

# Assignment 4: Simple DFS

***

UWB CSS 434  
Derek McLean  
June 7, 2012

***

### A. Documentation

Simple DFS sets up and maintains a simple distributed file system. This file system uses a single, central server that handles file requests from an arbitrary number of clients. Files are cached both server side and on the client.

The Implementation for Simple DFS is composed of three parts: client code (DFSClient), server code (DFSServer), and utility classes.

### Client

The client handles user interaction and maintains its own cache. The client always caches the latest file opened by a user since the application started.

The cache is maintained by a state machine with __5__ states:

1. __Invalid__: The cache does not have a copy of the file that can be read or written.
2. __Read_Shared__: The current file is available for read.
3. __Write_Owned__: The current file is owned for writting.
4. __Modified_Owned__: The current file is owned for writting and changes have recently been written to it.
5. __Release_Ownership__: The current file is being edited and must be written back as soon as possible.

The state __Modified\_Owned__ is an addition to the specification to handle writebacks. A client may 
receive a writeback signal from the server at one of two points: when the file is being edited and 
after the file is edited, but before a new file is opened. In the first case, we only want to write 
back when the editor closes, so we transition to release ownership. In the second case, we want to 
write back immediately since the user is not actively using the file. If it were still in Write\_Owned, 
the file would be uploaded when the user changes files. So, the file is put into modified Write\_Owned 
(__Modified\_Owned__) state that on writeback will upload changes immediately and transition to Read\_Shared.
If the same file is opened again for write, it transitions back to Write\_Owned.

<br>
<br>
<br>

Client handles two calls from the server:

1. __invalidate__: sets state to invalid. only called if client is registered to read
2. __writeback__: either writes back immediate or sets client to writeback as soon as the current editing session is complete.

Client also contains some user interface differences from the spec:

* It runs on an infinite loop that asks the user if they want to exit at the top of each iteration.
* It allows the user to select between vim, gvim, and emacs as their editor. This difference is due my greater familiarity of vim over emacs.

Finally, because of how the server works, when a client opens a file for write and another client owns the file for write, the current client will wait until the other client's editing session closes.

### Server

The server maintains the "filesystem" containing the files that clients desire to access. The server caches files that were opened during the course of execution.

The server responds to clients through two functions:

1. __download__: get a file. If the file is not cached, cache it.
2. __upload__: Latest written contents. Client releases write ownership, invalidates all readers.

Currently, the server uses a write-through policy for cached files. Changes to the cache are written back to the file on upload. This occurs at its own pace, in a separated thread.

Each cached file acts as a statemachine with __3__ states:

1. __NOT_SHARED__
    * No client is reading or writing the file.
    * owner is null
    * readers is empty
2. __READ_SHARED__
    * 1 or More clients are reading and writing the file.
    * owner is null
    * readers is not empty
3. __WRITE_OWNED__
    * A client is writing to the file. multiple clients may be reading the file.
    * owner is not null
    * readers state is unimportant

<br>
<br>
<br>

One state is missing, __Ownership\_Changed__. This state is reflected by the interaction between upload 
and download. When the write owner of a file is changed, the client issues a writeback to the old write 
owner. It waits until the owner is null. The writeback causes an upload to occur at some point in the 
future. This upload releases the owner's ownership, causing owner to be set to null. It notifies all download threads waiting for the owner to be null of the change. The first download thread to notice the change is the download that proceeds. This method avoids a deadlock present if the __Ownership_Changed__ state is used.

The server also responds to two commands:

* __list__ : list all cached files, their current owners and readers
* __exit__ : stop the server

### Utility Classes

These are extra classes to support the client and server. Namely, they are:

* __Prompter__: handles prompting for user input from stdin
* __AsyncFileWriter__: A thread for writing byte data to a file concurrent to the main thread.
* __File_Contents__: Stores the byte contents of a file

## B. Discussion

The first modification that could improve performance is the implementation of a write-on-exit policy for cached files on the server. Currently, the server uses a write-through. So, every upload is written to the file. Only the latest copy on exit needs to be uploaded.

Allowing clients to contact each other directly to transfer write-ownership could reduce network traffic. Currently, when ownership is transferred, the following steps have to take place:
* new owner contacts server
* server issues writeback to old owner
* old owner acknowledges writeback
* old owner uploads contents
* server acknowledges upload
* server replies to new owner with contents

If the current owner could be contacted directly, the client requesting ownership just needs to contact the server for the owner and the next owner, resulting in four network events. A similar system is used in xFS and later, resarch distributed file systems, which have a higher performance. This also would simplify the state machine on both client and server and handling deadlock might be simpler.

The wait-for-writeback on the server is sort of a spin loop. All threads waiting in this loop wake up each iteration and check if they can proceed. This is inefficient. If the synchronized keyword could be eliminated and a queue of waiting threads could be used with some sort of signaling mechanisim, it might improve performance with the wait.
