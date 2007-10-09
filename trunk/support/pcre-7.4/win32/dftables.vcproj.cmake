<?xml version="1.0" encoding = "Windows-1252"?>
<VisualStudioProject
	ProjectType="Visual C++"
	Version="7.10"
	Name="dftables"
	SccProjectName=""
	SccLocalPath=""
	Keyword="Win32Proj">
	<Platforms>
		<Platform
			Name="Win32"/>
	</Platforms>
	<Configurations>
		<Configuration
			Name="Debug|Win32"
			OutputDirectory="Debug"
			IntermediateDirectory="dftables.dir\Debug"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				BasicRuntimeChecks="1"
				CompileAs="1"
				DebugInformationFormat="3"
				ExceptionHandling="FALSE"
				InlineFunctionExpansion="0"
				Optimization="0"
				RuntimeLibrary="3"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,_DEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Debug\&quot;&quot;"
				AssemblerListingLocation="Debug"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/Debug/dftables.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				PreprocessorDefinitions="WIN32,_WINDOWS,_DEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Debug\&quot;&quot;"/>
			<Tool
				Name="VCMIDLTool"
				PreprocessorDefinitions="WIN32,_WINDOWS,_DEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Debug\&quot;&quot;"
				MkTypLibCompatible="FALSE"
				TargetEnvironment="1"
				GenerateStublessProxies="TRUE"
				TypeLibraryName="$(InputName).tlb"
				OutputDirectory="$(IntDir)"
				HeaderFileName="$(InputName).h"
				DLLDataFileName=""
				InterfaceIdentifierFileName="$(InputName)_i.c"
				ProxyFileName="$(InputName)_p.c"/>
			<Tool
				Name="VCPreBuildEventTool"/>
			<Tool
				Name="VCPreLinkEventTool"/>
			<Tool
				Name="VCPostBuildEventTool"/>
			<Tool
				Name="VCLinkerTool"
				AdditionalOptions=" /STACK:10000000 /machine:I386 /debug"
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib "
				OutputFile="Debug\dftables.exe"
				Version="0.0"
				LinkIncremental="2"
				AdditionalLibraryDirectories=""
				ProgramDataBaseFile="$(OutDir)\dftables.pdb"
				GenerateDebugInformation="TRUE"
				SubSystem="1"
/>
		</Configuration>
		<Configuration
			Name="Release|Win32"
			OutputDirectory="Release"
			IntermediateDirectory="dftables.dir\Release"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				CompileAs="1"
				ExceptionHandling="FALSE"
				InlineFunctionExpansion="2"
				Optimization="2"
				RuntimeLibrary="2"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Release\&quot;&quot;"
				AssemblerListingLocation="Release"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/Release/dftables.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Release\&quot;&quot;"/>
			<Tool
				Name="VCMIDLTool"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Release\&quot;&quot;"
				MkTypLibCompatible="FALSE"
				TargetEnvironment="1"
				GenerateStublessProxies="TRUE"
				TypeLibraryName="$(InputName).tlb"
				OutputDirectory="$(IntDir)"
				HeaderFileName="$(InputName).h"
				DLLDataFileName=""
				InterfaceIdentifierFileName="$(InputName)_i.c"
				ProxyFileName="$(InputName)_p.c"/>
			<Tool
				Name="VCPreBuildEventTool"/>
			<Tool
				Name="VCPreLinkEventTool"/>
			<Tool
				Name="VCPostBuildEventTool"/>
			<Tool
				Name="VCLinkerTool"
				AdditionalOptions=" /STACK:10000000 /machine:I386"
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib "
				OutputFile="Release\dftables.exe"
				Version="0.0"
				LinkIncremental="1"
				AdditionalLibraryDirectories=""
				ProgramDataBaseFile="$(OutDir)\dftables.pdb"
				SubSystem="1"
/>
		</Configuration>
		<Configuration
			Name="MinSizeRel|Win32"
			OutputDirectory="MinSizeRel"
			IntermediateDirectory="dftables.dir\MinSizeRel"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				CompileAs="1"
				ExceptionHandling="FALSE"
				InlineFunctionExpansion="1"
				Optimization="1"
				RuntimeLibrary="2"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;MinSizeRel\&quot;&quot;"
				AssemblerListingLocation="MinSizeRel"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/MinSizeRel/dftables.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;MinSizeRel\&quot;&quot;"/>
			<Tool
				Name="VCMIDLTool"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;MinSizeRel\&quot;&quot;"
				MkTypLibCompatible="FALSE"
				TargetEnvironment="1"
				GenerateStublessProxies="TRUE"
				TypeLibraryName="$(InputName).tlb"
				OutputDirectory="$(IntDir)"
				HeaderFileName="$(InputName).h"
				DLLDataFileName=""
				InterfaceIdentifierFileName="$(InputName)_i.c"
				ProxyFileName="$(InputName)_p.c"/>
			<Tool
				Name="VCPreBuildEventTool"/>
			<Tool
				Name="VCPreLinkEventTool"/>
			<Tool
				Name="VCPostBuildEventTool"/>
			<Tool
				Name="VCLinkerTool"
				AdditionalOptions=" /STACK:10000000 /machine:I386"
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib "
				OutputFile="MinSizeRel\dftables.exe"
				Version="0.0"
				LinkIncremental="1"
				AdditionalLibraryDirectories=""
				ProgramDataBaseFile="$(OutDir)\dftables.pdb"
				SubSystem="1"
/>
		</Configuration>
		<Configuration
			Name="RelWithDebInfo|Win32"
			OutputDirectory="RelWithDebInfo"
			IntermediateDirectory="dftables.dir\RelWithDebInfo"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				CompileAs="1"
				DebugInformationFormat="3"
				ExceptionHandling="FALSE"
				InlineFunctionExpansion="1"
				Optimization="2"
				RuntimeLibrary="2"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;RelWithDebInfo\&quot;&quot;"
				AssemblerListingLocation="RelWithDebInfo"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/RelWithDebInfo/dftables.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4&quot;;"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;RelWithDebInfo\&quot;&quot;"/>
			<Tool
				Name="VCMIDLTool"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;RelWithDebInfo\&quot;&quot;"
				MkTypLibCompatible="FALSE"
				TargetEnvironment="1"
				GenerateStublessProxies="TRUE"
				TypeLibraryName="$(InputName).tlb"
				OutputDirectory="$(IntDir)"
				HeaderFileName="$(InputName).h"
				DLLDataFileName=""
				InterfaceIdentifierFileName="$(InputName)_i.c"
				ProxyFileName="$(InputName)_p.c"/>
			<Tool
				Name="VCPreBuildEventTool"/>
			<Tool
				Name="VCPreLinkEventTool"/>
			<Tool
				Name="VCPostBuildEventTool"/>
			<Tool
				Name="VCLinkerTool"
				AdditionalOptions=" /STACK:10000000 /machine:I386 /debug"
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib "
				OutputFile="RelWithDebInfo\dftables.exe"
				Version="0.0"
				LinkIncremental="2"
				AdditionalLibraryDirectories=""
				ProgramDataBaseFile="$(OutDir)\dftables.pdb"
				GenerateDebugInformation="TRUE"
				SubSystem="1"
/>
		</Configuration>
	</Configurations>
	<Files>
			<File
				RelativePath="C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt">
				<FileConfiguration
					Name="Debug|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; &quot;-HC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4&quot; &quot;-BC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32&quot;"
					AdditionalDependencies="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeSystem.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCCompiler.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeRCCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\config-cmake.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre.h.generic&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre_stringpiece.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcrecpparg.h.in&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
				<FileConfiguration
					Name="Release|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; &quot;-HC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4&quot; &quot;-BC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32&quot;"
					AdditionalDependencies="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeSystem.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCCompiler.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeRCCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\config-cmake.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre.h.generic&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre_stringpiece.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcrecpparg.h.in&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
				<FileConfiguration
					Name="MinSizeRel|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; &quot;-HC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4&quot; &quot;-BC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32&quot;"
					AdditionalDependencies="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeSystem.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCCompiler.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeRCCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\config-cmake.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre.h.generic&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre_stringpiece.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcrecpparg.h.in&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
				<FileConfiguration
					Name="RelWithDebInfo|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; &quot;-HC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4&quot; &quot;-BC:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32&quot;"
					AdditionalDependencies="&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeSystem.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCCompiler.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeRCCompiler.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCPlatform.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\win32\CMakeFiles\CMakeCXXPlatform.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\config-cmake.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre.h.generic&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcre_stringpiece.h.in&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\pcrecpparg.h.in&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;&quot;C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\CMakeLists.txt&quot;;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
			</File>
		<Filter
			Name="Source Files"
			Filter="">
			<File
				RelativePath="C:\Documents and Settings\kru028.NEXUS\Desktop\braccetto code\avis\support\pcre-7.4\dftables.c">
			</File>
		</Filter>
	</Files>
	<Globals>
	</Globals>
</VisualStudioProject>
