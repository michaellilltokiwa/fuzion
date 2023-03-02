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


#ifndef	_FUZION_H
#define	_FUZION_H	1

#include <errno.h>
#include <stdio.h>
#include <string.h>

#include <stdlib.h>     // setenv, unsetenv
#include <sys/stat.h>   // mkdir
#include <sys/types.h>  // mkdir


#if _WIN32

// "For example if you want to use winsock2.h you better make sure
// WIN32_LEAN_AND_MEAN is always defined because otherwise you will
// get conflicting declarations between the WinSock versions."
// https://stackoverflow.com/questions/11040133/what-does-defining-win32-lean-and-mean-exclude-exactly#comment108482188_11040230
#define WIN32_LEAN_AND_MEAN

#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>

#else
#include <sys/socket.h> // socket, bind, listen, accept, connect
#include <sys/ioctl.h>  // ioctl, FIONREAD
#include <netinet/in.h> // AF_INET
#include <poll.h>       // poll
#include <fcntl.h>      // fcntl
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
int fzE_set_blocking(int fd, unsigned long blocking)
{
#ifdef _WIN32
  if ( ioctlsocket(fd, FIONBIO, &blocking) == -1 ) {
    return -1;
  }
#else
  if ( fcntl(fd, F_SETFL, blocking == 1 ? fcntl(fd, F_GETFL, 0) | O_NONBLOCK : fcntl(fd, F_GETFL, 0) & ~O_NONBLOCK) == -1 ) {
    return -1;
  }
#endif
  return fd;
}


int fzE_socket(int domain, int type, int protocol){
#ifdef _WIN32
  WSADATA wsaData;
  if ( WSAStartup(MAKEWORD(2,2), &wsaData) != 0 ) {
    return -1;
  }
#endif
  return socket(domain, type, protocol);
}

int fzE_net_error()
{
#ifdef _WIN32
  return WSAGetLastError();
#else
  return errno;
#endif
}

int fzE_bind(int sockfd, int family, char * data, int data_len){
  struct sockaddr sa;
  memset(&sa, 0, sizeof sa);
  sa.sa_family = family;
  memcpy(sa.sa_data, data, data_len);
  return ( bind(sockfd, &sa, sizeof sa) == -1 )
    ? fzE_net_error()
    : 0;
}

int fzE_listen(int sockfd, int backlog){
  return ( listen(sockfd, backlog) == -1 )
    ? fzE_net_error()
    : 0;
}

int fzE_accept(int sockfd){
  return accept(sockfd, NULL, NULL);
}

int fzE_connect(int sockfd, int family, char * data, int data_len){
  struct sockaddr sa;
  memset(&sa, 0, sizeof sa);
  sa.sa_family = family;
  memcpy(sa.sa_data, data, data_len);
  return ( connect(sockfd, &sa, sizeof sa) == -1 )
    ? fzE_net_error()
    : 0;
}

int fzE_read(int sockfd, void * buf, size_t count){
  int res = recv( sockfd, buf, count, 0 );
// NYI set none blocking after first ready is probably not
// what we want...
  fzE_set_blocking(sockfd, 1);
  return res;
}

int fzE_write(int sockfd, const void * buf, size_t count){
return ( send( sockfd, buf, count, 0 ) == -1 )
  ? fzE_net_error()
  : 0;
}

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


size_t fzE_bytes_available(int fd, size_t buf_size)
{
  size_t bytes_count = 0;
#ifdef _WIN32
  ioctlsocket(fd, FIONREAD, (u_long *)(&bytes_count));
#else
  struct pollfd pfds = {fd, POLLIN, POLLIN};
  poll(&pfds, 1, -1);
  ioctl(fd, FIONREAD, &bytes_count);
#endif
  return (bytes_count < buf_size) ?  bytes_count : buf_size;
}


#endif /* fz.h  */
