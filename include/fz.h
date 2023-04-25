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

#if _WIN32
#include <namedpipeapi.h>
#else
#include <stdio.h> // fdopen
#include <spawn.h>      // posix_spawn
#include <unistd.h>     // pipe
#include <sys/wait.h>   // wait
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


// create pipe
int fzE_create_pipe(int pipes[]){
#if _WIN32
  return CreatePipe(&pipes[0], &pipes[1], NULL, 0) ? 0 : -1;
#else
  return pipe(pipes);
#endif
}


// wait for process to finish
void fzE_process_wait(int64_t p){
#if _WIN32
  WaitForSingleObject(p, INFINITE );
#else
int status;
do {
  if (waitpid(p, &status, WUNTRACED | WCONTINUED) == -1)
      exit(1);
} while (WIFCONTINUED(status));
#endif
}


// zero on success, -1 error
int fzE_process_create(char * cmdLine, int64_t * result){

  int stdIn[2];
  int stdOut[2];
  int stdErr[2];
  if (
       fzE_create_pipe(stdIn) == -1
    || fzE_create_pipe(stdErr) == -1
    || fzE_create_pipe(stdOut) == -1
   )
  {
    return -1;
  }
#if _WIN32
  PROCESS_INFORMATION processInfo;
  ZeroMemory( &processInfo, sizeof(PROCESS_INFORMATION) );
  STARTUPINFO startupInfo;
  ZeroMemory( &startupInfo, sizeof(STARTUPINFO) );
  startupInfo.cb = sizeof(STARTUPINFO);
  startupInfo.hStdInput = stdIn[0];
  startupInfo.hStdOutput = stdOut[1];
  startupInfo.hStdError = stdErr[1];
  startupInfo.dwFlags |= STARTF_USESTDHANDLES;

  if( !CreateProcess(NULL,
      TEXT(cmdLine), // command line
      NULL,          // process security attributes
      NULL,          // primary thread security attributes
      TRUE,          // handles are inherited
      0,             // creation flags
      NULL,          // use parent's environment
      NULL,          // use parent's current directory
      &startupInfo,  // STARTUPINFO pointer
      &processInfo)) // receives PROCESS_INFORMATION
  {
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

  CloseHandle(stdIn[0]);
  CloseHandle(stdOut[1]);
  CloseHandle(stdErr[1]);

  result[0] = processInfo.hProcess;
  result[1] = (int64_t) fdopen(stdIn[1], "w");
  result[2] = (int64_t) fdopen(stdOut[0], "r");
  result[3] = (int64_t) fdopen(stdErr[0], "r");
  return 0;
#else
  pid_t processId;

  posix_spawn_file_actions_t file_actions;

  if (posix_spawn_file_actions_init(&file_actions) != 0)
    exit(1);

  posix_spawn_file_actions_addclose(&file_actions, stdIn[1]);
  posix_spawn_file_actions_addclose(&file_actions, stdOut[0]);
  posix_spawn_file_actions_addclose(&file_actions, stdErr[0]);
  posix_spawn_file_actions_adddup2(&file_actions, stdIn[0], 0);
  posix_spawn_file_actions_adddup2(&file_actions, stdOut[1], 1);
  posix_spawn_file_actions_adddup2(&file_actions, stdErr[1], 2);
  posix_spawn_file_actions_addclose(&file_actions, stdIn[0]);
  posix_spawn_file_actions_addclose(&file_actions, stdOut[1]);
  posix_spawn_file_actions_addclose(&file_actions, stdErr[1]);

  char * const args[] = {cmdLine, NULL};

  int s = posix_spawn(
        &processId,
        args[0],
        &file_actions,
        NULL,
        args, // args
        NULL  // environment
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
  ReadFile((HANDLE)desc, );
#else
  return read((int) desc, buf, nbytes);
#endif
}


int fzE_pipe_write(int64_t desc, char * buf, size_t nbytes){
#if _WIN32
  WriteFile((HANDLE)desc, );
#else
  return write((int) desc, buf, nbytes);
#endif
}


int fzE_pipe_close(int64_t desc){
#if _WIN32
  CloseHandle((HANDLE)desc);
#else
  return close((int) desc);
#endif
}


#endif /* fz.h  */
