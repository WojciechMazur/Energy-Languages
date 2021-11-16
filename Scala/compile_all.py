import sys, os
from subprocess import call, check_output, Popen, PIPE
from lazyme.string import color_print

path = '.'
action = 'compile'
arguments = ''
supportedCommands = ['compile', 'run', 'test', 'measure', 'measureWithWarmup', 'mem', 'valgrind', 'clean']

def file_exists(file_path):
    if not file_path:
        return False
    else:
        return os.path.isfile(file_path)

def main():
  for root, dirs, files in os.walk(path):
    makefile = os.path.join(root, "Makefile")
    if file_exists(makefile):
      print('Checking ' + root)
      cmd = 'cd ' + root + '; make ' + action + ' ' + arguments
      pipes = Popen(cmd, shell=True, stdout=PIPE, stderr=PIPE)
      std_out, std_err = pipes.communicate()
      
      if action in ['compile', 'run', 'test']:
        if pipes.returncode != 0:
          # an error happened!
          err_msg = "%s. Code: %s" % (std_err.strip(), pipes.returncode)
          color_print('[E] Error on ' + root + ': ', color='red', bold=True)
          print(err_msg)
        elif len(std_err):
          # return code is 0 (no error), but we may want to
          # do something with the info on std_err
          # i.e. logger.warning(std_err)
          color_print('[OK]', color='green')
        else:
          color_print('[OK]', color='green')
      if action == 'measure':
        call(['sleep', '5'])

if __name__ == '__main__':
  if len(sys.argv) >= 2:
    act = sys.argv[1]
    if act in supportedCommands:
      color_print('Performing \"' + act + '\" action...', color='yellow', bold=True)
      action = act
      arguments =  ' '.join(map(str,sys.argv[2:]))
      if arguments:
        color_print('Passed arguments: ' + arguments, color='yellow', bold=True)
    else:
      color_print('Performing \"compile\" action...', color='yellow', bold=True)
      arguments =  ' '.join(map(str,sys.argv[1:]))
      if arguments:
        color_print('Passed arguments: ' + str(arguments), color='yellow', bold=True)
  else:
    color_print('Performing \"compile\" action...', color='yellow', bold=True)
    action = 'compile'
  
  main()
    
