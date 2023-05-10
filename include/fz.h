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

#include <stdlib.h>     // setenv, unsetenv
#include <sys/stat.h>   // mkdir
#include <sys/types.h>  // mkdir
#include <stdint.h>

#if _WIN32
#include <synchapi.h> // WaitForSingleObject
#include <windows.h>
#include <namedpipeapi.h>
#else
#include <stdio.h>      // fdopen
#include <spawn.h>      // posix_spawn
#include <unistd.h>     // pipe
#include <sys/wait.h>   // wait
#include <fcntl.h>      // O_NONBLOCK
#include <poll.h>       // poll
#include <errno.h>
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


// zero on success, -1 error
int fzE_process_create(char * args[], size_t argsLen, char * env[], size_t envLen, int64_t * result, char * args_str, char * env_str) {
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

  // without this, process reading from stdin
  // hang even when stdin is closed by parent process
  DWORD mode = PIPE_NOWAIT;
  SetNamedPipeHandleState(stdIn[0], &mode, NULL, NULL);
  SetNamedPipeHandleState(stdOut[1], &mode, NULL, NULL);
  SetNamedPipeHandleState(stdErr[1], &mode, NULL, NULL);

  // prepare create process args
  PROCESS_INFORMATION processInfo;
  ZeroMemory( &processInfo, sizeof(PROCESS_INFORMATION) );
  STARTUPINFO startupInfo;
  ZeroMemory( &startupInfo, sizeof(STARTUPINFO) );
  startupInfo.cb = sizeof(STARTUPINFO);
  startupInfo.hStdInput = stdIn[0];
  startupInfo.hStdOutput = stdOut[1];
  startupInfo.hStdError = stdErr[1];
  startupInfo.dwFlags |= STARTF_USESTDHANDLES;

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
      TRUE,                          // handles are inherited
      0,                             // creation flags
      env_str,                       // environment
      NULL,                          // use parent's current directory
      &startupInfo,                  // STARTUPINFO pointer
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
  // https://www.microsoft.com/en-us/research/publication/a-fork-in-the-road/

  int stdIn[2];
  int stdOut[2];
  int stdErr[2];
  if ( pipe(stdIn ) == -1)
  {
    return -1;
  }
  if (pipe(stdOut) == -1)
  {
    close(stdIn[0]);
    close(stdIn[1]);
    return -1;
  }
  if(pipe(stdErr) == -1)
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
    return -1;
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


#endif /* fz.h  */
