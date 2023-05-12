/*

This file is part of the Fuzion language implementation.

The Fuzion language implementation is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, version 3 of the License.

The Fuzion language implementation is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License along with The
Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.

*/

/*-----------------------------------------------------------------------
 *
 * Tokiwa Software GmbH, Germany
 *
 * Source of main include of Fuzion C backend.
 *
 *---------------------------------------------------------------------*/


#ifndef _FUZION_H
#define _FUZION_H 1


#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>     // setenv, unsetenv
#include <string.h>

#if _WIN32

// "For example if you want to use winsock2.h you better make sure
// WIN32_LEAN_AND_MEAN is always defined because otherwise you will
// get conflicting declarations between the WinSock versions."
// https://stackoverflow.com/questions/11040133/what-does-defining-win32-lean-and-mean-exclude-exactly#comment108482188_11040230
#define WIN32_LEAN_AND_MEAN

#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>

#include <synchapi.h> // WaitForSingleObject
#include <namedpipeapi.h>

#else

#include <errno.h>
#include <fcntl.h>      // fcntl, O_NONBLOCK
#include <netdb.h>      // getaddrinfo
#include <netinet/in.h> // AF_INET
#include <poll.h>       // poll
#include <spawn.h>      // posix_spawn
#include <sys/ioctl.h>  // ioctl, FIONREAD
#include <sys/socket.h> // socket, bind, listen, accept, connect
#include <sys/stat.h>   // mkdir
#include <sys/types.h>
#include <sys/wait.h>   // wait
#include <unistd.h>     // close, pipe

#endif



// make directory, return zero on success
int fzE_mkdir(const char *pathname){
#if _WIN32
  // should we CreateDirectory here?
  return mkdir(pathname);
#else
  return mkdir(pathname, S_IRWXU);
#endif
}



// set environment variable, return zero on success
int fzE_setenv(const char *name, const char *value, int overwrite){
#if _WIN32
  // setenv is posix only
  return -1;
#else
  return setenv(name, value, overwrite);
#endif
}



// unset environment variable, return zero on success
int fzE_unsetenv(const char *name){
#if _WIN32
  // unsetenv is posix only
  return -1;
#else
  return unsetenv(name);
#endif
}

// 0 = blocking
// 1 = none_blocking
int fzE_set_blocking(int sockfd, int blocking)
{
#ifdef _WIN32
  u_long b = blocking;
  return ioctlsocket(sockfd, FIONBIO, &b);
#else
  int flag = blocking == 1
    ? fcntl(sockfd, F_GETFL, 0) | O_NONBLOCK
    : fcntl(sockfd, F_GETFL, 0) & ~O_NONBLOCK;

  return fcntl(sockfd, F_SETFL, flag);
#endif
}

// helper function to retrieve
// the last error that occured.
int fzE_net_error()
{
#ifdef _WIN32
  return WSAGetLastError();
#else
  return errno;
#endif
}

// fuzion family number -> system family number
int get_family(int family)
{
  return family == 1
    ? AF_UNIX
    : family == 2
    ? AF_INET
    : family == 10
    ? AF_INET6
    : -1;
}

// fuzion socket type number -> system socket type number
int get_socket_type(int socktype)
{
  return socktype == 1
    ? SOCK_STREAM
    : socktype == 2
    ? SOCK_DGRAM
    : socktype == 3
    ? SOCK_RAW
    : -1;
}

// fuzion protocol number -> system protocol number
int get_protocol(int protocol)
{
  return protocol == 6
    ? IPPROTO_TCP
    : protocol == 17
    ? IPPROTO_UDP
    : protocol == 0
    ? IPPROTO_IP
    : protocol == 41
    ? IPPROTO_IPV6
    : -1;
}

// close a socket descriptor
int fzE_close(int sockfd)
{
#ifdef _WIN32
  closesocket(sockfd);
  WSACleanup();
  return fzE_net_error();
#else
  return ( close(sockfd) == - 1 )
    ? fzE_net_error()
    : 0;
#endif
}

// initialize a new socket for given
// family, socket_type, protocol
int fzE_socket(int family, int type, int protocol){
#ifdef _WIN32
  WSADATA wsaData;
  if ( WSAStartup(MAKEWORD(2,2), &wsaData) != 0 ) {
    return -1;
  }
#endif
  // NYI use lock to make this _atomic_.
  int sockfd = socket(get_family(family), get_socket_type(type), get_protocol(protocol));
#ifndef _WIN32
  fcntl(sockfd, F_SETFD, FD_CLOEXEC);
#endif
  return sockfd;
}


// get addrinfo structure used for binding/connection of a socket.
int fzE_getaddrinfo(int family, int socktype, int protocol, int flags, char * host, char * port, struct addrinfo ** result){
  struct addrinfo hints;

#ifdef _WIN32
  ZeroMemory(&hints, sizeof(hints));
#else
  memset(&hints, 0, sizeof hints);
#endif
  hints.ai_family = get_family(family);
  hints.ai_socktype = get_socket_type(socktype);
  hints.ai_protocol = get_protocol(protocol);
  hints.ai_flags = flags;

  return getaddrinfo(host, port, &hints, result);
}


// create a new socket and bind to given host:port
// result[0] contains either an errorcode or a socket descriptor
// -1 error, 0 success
int fzE_bind(int family, int socktype, int protocol, char * host, char * port, int64_t * result){
  result[0] = fzE_socket(family, socktype, protocol);
  if (result[0] == -1)
  {
    result[0] = fzE_net_error();
    return -1;
  }
  struct addrinfo *addr_info = NULL;
  int addrRes = fzE_getaddrinfo(family, socktype, protocol, AI_PASSIVE, host, port, &addr_info);
  if (addrRes != 0)
  {
    fzE_close(result[0]);
    result[0] = addrRes;
    return -1;
  }
  int bind_res = bind(result[0], addr_info->ai_addr, (int)addr_info->ai_addrlen);

  if(bind_res == -1)
  {
    fzE_close(result[0]);
    result[0] = fzE_net_error();
    return -1;
  }
  freeaddrinfo(addr_info);
  return bind_res;
}

// set the given socket to listening
// backlog = queuelength of pending connections
int fzE_listen(int sockfd, int backlog){
  return ( listen(sockfd, backlog) == -1 )
    ? fzE_net_error()
    : 0;
}

// accept a new connection
// blocks if socket is blocking
int fzE_accept(int sockfd){
  return accept(sockfd, NULL, NULL);
}


// create connection for given parameters
// result[0] contains either an errorcode or a socket descriptor
// -1 error, 0 success
int fzE_connect(int family, int socktype, int protocol, char * host, char * port, int64_t * result){
  // get socket
  result[0] = fzE_socket(family, socktype, protocol);
  if (result[0] == -1)
  {
    result[0] = fzE_net_error();
    return -1;
  }
  struct addrinfo *addr_info = NULL;
  int addrRes = fzE_getaddrinfo(family, socktype, protocol, 0, host, port, &addr_info);
  if (addrRes != 0)
  {
    fzE_close(result[0]);
    result[0] = addrRes;
    return -1;
  }
  int con_res = connect(result[0], addr_info->ai_addr, addr_info->ai_addrlen);
  if(con_res == -1)
  {
    // NYI do we want to try another address in addr_info->ai_next?
    fzE_close(result[0]);
    result[0] = fzE_net_error();
  }
  freeaddrinfo(addr_info);
  return con_res;
}

// read up to count bytes bytes from sockfd
// into buf. may block if socket is  set to blocking.
// return -1 on error or number of bytes read
int fzE_read(int sockfd, void * buf, size_t count){
#ifdef _WIN32
  int rec_res = recvfrom( sockfd, buf, count, 0, NULL, NULL );
  if (rec_res == -1)
  {
    // silently discard rest to
    // match behaviour on linux
    return fzE_net_error() == WSAEMSGSIZE
      ? count
      : rec_res;
  }
  return rec_res;
#else
  return recvfrom( sockfd, buf, count, 0, NULL, NULL );
#endif
}

// write buf to sockfd
// may block if socket is set to blocking.
// return error code or zero on success
int fzE_write(int sockfd, const void * buf, size_t count){
return ( sendto( sockfd, buf, count, 0, NULL, 0 ) == -1 )
  ? fzE_net_error()
  : 0;
}



// wait for process to finish, cleanup resources
uint32_t fzE_process_wait(int64_t p){
#if _WIN32
  DWORD status = 0;
  WaitForSingleObject((HANDLE)p, INFINITE);
  if (!GetExitCodeProcess((HANDLE)p, &status)){
    exit(1);
  }
  CloseHandle((HANDLE)p);
  return (uint32_t)status;
#else
  int status;
  do {
    if (waitpid(p, &status, WUNTRACED | WCONTINUED) == -1)
        exit(1);
  } while (WIFCONTINUED(status));
  return WIFEXITED(status)
    ? 0
    : WEXITSTATUS(status);
#endif
}

// NYI make this thread safe
// NYI option to pass stdin,stdout,stderr
// zero on success, -1 error
int fzE_process_create(char * args[], size_t argsLen, char * env[], size_t envLen, int64_t * result, char * args_str, char * env_str) {

  // NYI
  // Describes the how and why
  // of making file descriptors, handlers, sockets
  // none inheritable (CLOEXEC, HANDLE_FLAG_INHERIT):
  // https://peps.python.org/pep-0446/

#if _WIN32
  // create stdIn, stdOut, stdErr pipes
  HANDLE stdIn[2];
  HANDLE stdOut[2];
  HANDLE stdErr[2];

  SECURITY_ATTRIBUTES secAttr = { sizeof(SECURITY_ATTRIBUTES) , NULL, TRUE };

  // NYI cleanup on error
  if ( !CreatePipe(&stdIn[0], &stdIn[1], &secAttr, 0)
    || !CreatePipe(&stdOut[0], &stdOut[1],&secAttr, 0)
    || !CreatePipe(&stdErr[0], &stdErr[1], &secAttr, 0))
  {
    return -1;
  }

  // prepare create process args
  PROCESS_INFORMATION processInfo;
  ZeroMemory( &processInfo, sizeof(PROCESS_INFORMATION) );
  STARTUPINFO startupInfo;
  ZeroMemory( &startupInfo, sizeof(STARTUPINFO) );
  startupInfo.hStdInput = stdIn[0];
  startupInfo.hStdOutput = stdOut[1];
  startupInfo.hStdError = stdErr[1];
  startupInfo.dwFlags |= STARTF_USESTDHANDLES;

  // Programmatically controlling which handles are inherited by new processes in Win32
  // https://devblogs.microsoft.com/oldnewthing/20111216-00/?p=8873

  SIZE_T size = 0;
  LPPROC_THREAD_ATTRIBUTE_LIST lpAttributeList = NULL;
  if(! (InitializeProcThreadAttributeList(NULL, 1, 0, &size) ||
             GetLastError() == ERROR_INSUFFICIENT_BUFFER)){
    return -1;
  }
  lpAttributeList = (LPPROC_THREAD_ATTRIBUTE_LIST) HeapAlloc(GetProcessHeap(), 0, size);
  if(lpAttributeList == NULL){
    return -1;
  }
  if(!InitializeProcThreadAttributeList(lpAttributeList,
                      1, 0, &size)){
    HeapFree(GetProcessHeap(), 0, lpAttributeList);
    return -1;
  }
  HANDLE handlesToInherit[] =  { stdIn[0], stdOut[1], stdErr[1] };
  if(!UpdateProcThreadAttribute(lpAttributeList,
                      0, PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
                      handlesToInherit,
                      3 * sizeof(HANDLE), NULL, NULL)){
    DeleteProcThreadAttributeList(lpAttributeList);
    HeapFree(GetProcessHeap(), 0, lpAttributeList);
    return -1;
  }

  STARTUPINFOEX startupInfoEx;
  ZeroMemory( &startupInfoEx, sizeof(startupInfoEx) );
  startupInfoEx.StartupInfo = startupInfo;
  startupInfoEx.StartupInfo.cb = sizeof(startupInfoEx);
  startupInfoEx.lpAttributeList = lpAttributeList;

  // NYI use unicode?
  // int wchars_num = MultiByteToWideChar(CP_UTF8, 0, &str, -1, NULL, 0);
  // wchar_t* wstr = new wchar_t[wchars_num];
  // MultiByteToWideChar(CP_UTF8, 0, &str, -1, wstr, wchars_num);
  // Note that an ANSI environment block is terminated by two zero bytes: one for the last string, one more to terminate the block.
  // A Unicode environment block is terminated by four zero bytes: two for the last string, two more to terminate the block.


  if( !CreateProcess(NULL,
      TEXT(args_str),                // command line
      NULL,                          // process security attributes
      NULL,                          // primary thread security attributes
      TRUE,                          // inherit handles listed in startupInfo
      EXTENDED_STARTUPINFO_PRESENT,  // creation flags
      env_str,                       // environment
      NULL,                          // use parent's current directory
      &startupInfoEx.StartupInfo,    // STARTUPINFOEX pointer
      &processInfo))                 // receives PROCESS_INFORMATION
  {
    // cleanup all pipes
    CloseHandle(stdIn[0]);
    CloseHandle(stdIn[1]);
    CloseHandle(stdOut[0]);
    CloseHandle(stdOut[1]);
    CloseHandle(stdErr[0]);
    CloseHandle(stdErr[1]);
    return -1;
  }

   DeleteProcThreadAttributeList(lpAttributeList);
   HeapFree(GetProcessHeap(), 0, lpAttributeList);

  // no need for this handle, closing
  CloseHandle(processInfo.hThread);

  // close the handles given to child process.
  CloseHandle(stdIn[0]);
  CloseHandle(stdOut[1]);
  CloseHandle(stdErr[1]);

  result[0] = (int64_t) processInfo.hProcess;
  result[1] = (int64_t) stdIn[1];
  result[2] = (int64_t) stdOut[0];
  result[3] = (int64_t) stdErr[0];
  return 0;
#else
  // Some problems with fork, exec:
  // https://www.microsoft.com/en-us/research/publication/a-fork-in-the-road/

  int stdIn[2];
  int stdOut[2];
  int stdErr[2];
  if (pipe(stdIn ) == -1)
  {
    return -1;
  }
  if (pipe(stdOut) == -1)
  {
    close(stdIn[0]);
    close(stdIn[1]);
    return -1;
  }
  if (pipe(stdErr) == -1)
  {
    close(stdIn[0]);
    close(stdIn[1]);
    close(stdOut[0]);
    close(stdOut[1]);
    return -1;
  }

  fcntl(stdIn[1], F_SETFD, FD_CLOEXEC);
  fcntl(stdOut[0], F_SETFD, FD_CLOEXEC);
  fcntl(stdErr[0], F_SETFD, FD_CLOEXEC);

  pid_t processId;

  posix_spawn_file_actions_t file_actions;

  if (posix_spawn_file_actions_init(&file_actions) != 0)
    exit(1);

  posix_spawn_file_actions_adddup2(&file_actions, stdIn[0], 0);
  posix_spawn_file_actions_adddup2(&file_actions, stdOut[1], 1);
  posix_spawn_file_actions_adddup2(&file_actions, stdErr[1], 2);
  posix_spawn_file_actions_addclose(&file_actions, stdIn[0]);
  posix_spawn_file_actions_addclose(&file_actions, stdOut[1]);
  posix_spawn_file_actions_addclose(&file_actions, stdErr[1]);

  args[argsLen -1] = NULL;
  env[envLen -1] = NULL;

  int s = posix_spawnp(
        &processId,
        args[0],
        &file_actions,
        NULL,
        args, // args
        env  // environment
        );

  close(stdIn[0]);
  close(stdOut[1]);
  close(stdErr[1]);

  posix_spawn_file_actions_destroy(&file_actions);

  if(s != 0)
    {
      close(stdIn[0]);
      close(stdIn[1]);
      close(stdOut[0]);
      close(stdOut[1]);
      close(stdErr[0]);
      close(stdErr[1]);
      return -1;
    }

  result[0] = processId;
  result[1] = (int64_t) stdIn[1];
  result[2] = (int64_t) stdOut[0];
  result[3] = (int64_t) stdErr[0];
  return 0;
#endif
}


int fzE_pipe_read(int64_t desc, char * buf, size_t nbytes){
#if _WIN32
  DWORD bytesRead;
  if (!ReadFile((HANDLE)desc, buf, nbytes, &bytesRead, NULL)){
    return GetLastError() == ERROR_BROKEN_PIPE
      ? 0
      : -1;
  }
  return bytesRead;
#else
  return read((int) desc, buf, nbytes);
#endif
}


int fzE_pipe_write(int64_t desc, char * buf, size_t nbytes){
#if _WIN32
  DWORD bytesWritten;
  if (!WriteFile((HANDLE)desc, buf, nbytes, &bytesWritten, NULL)){
    return -1;
  }
  return bytesWritten;
#else
  return write((int) desc, buf, nbytes);
#endif
}


int fzE_pipe_close(int64_t desc){
// NYI do we need to flush?
#if _WIN32
  return CloseHandle((HANDLE)desc)
    ? 0
    : -1;
#else
  return close((int) desc);
#endif
}


void fzE_file_open(char * file_name, int64_t * open_results, int8_t mode)
{
  // NYI use lock to make fopen and fcntl _atomic_.
  FILE * fp;
  errno = 0;
  switch (mode)
  {
    case 0:
    {
      fp = fopen(file_name,"rb");
      if (fp!=NULL)
      {
        open_results[0] = (int64_t)fp;
      }
      break;
    }
    case 1:
    {
      fp = fopen(file_name,"wb");
      if (fp!=NULL)
      {
        open_results[0] = (int64_t)fp;
      }
      break;
    }
    case 2:
    {
      fp = fopen(file_name,"ab");
      if (fp!=NULL)
      {
        open_results[0] = (int64_t)fp;
      }
      break;
    }
    default:
    {
      fprintf(stderr,"*** Unsupported open flag. Please use: 0 for READ, 1 for WRITE, 2 for APPEND. ***\012");
      exit(1);
    }
  }
#ifndef _WIN32
  fcntl(open_results[0], F_SETFD, FD_CLOEXEC);
#endif
  open_results[1] = (int64_t)errno;
}


#endif /* fz.h  */
