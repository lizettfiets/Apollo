'Start apl-blockchain in background without open console window
'Required for Windows installer
Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName) & "\apl-run.bat" & chr(34) & " -s", 0,
false