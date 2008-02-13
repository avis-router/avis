<?xml version="1.0" encoding = "Windows-1252"?>
<VisualStudioProject
	ProjectType="Visual C++"
	Version="7.10"
	Name="pcre_scanner_unittest"
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
			IntermediateDirectory="pcre_scanner_unittest.dir\Debug"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories=".;..;"
				BasicRuntimeChecks="1"
				CompileAs="2"
				DebugInformationFormat="3"
				ExceptionHandling="TRUE"
				InlineFunctionExpansion="0"
				Optimization="0"
				RuntimeLibrary="3"
				RuntimeTypeInfo="TRUE"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,_DEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Debug\&quot;&quot;"
				AssemblerListingLocation="Debug"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/Debug/pcre_scanner_unittest.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories=".;..;"
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
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib pcrecpp.lib pcre.lib "
				OutputFile="Debug\pcre_scanner_unittest.exe"
				Version="0.0"
				LinkIncremental="2"
				AdditionalLibraryDirectories=".\$(OutDir),."
				ProgramDataBaseFile="$(OutDir)\pcre_scanner_unittest.pdb"
				GenerateDebugInformation="TRUE"
				SubSystem="1"
				StackReserveSize="10000000"/>
		</Configuration>
		<Configuration
			Name="Release|Win32"
			OutputDirectory="Release"
			IntermediateDirectory="pcre_scanner_unittest.dir\Release"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories=".;..;"
				CompileAs="2"
				ExceptionHandling="TRUE"
				InlineFunctionExpansion="2"
				Optimization="2"
				RuntimeLibrary="2"
				RuntimeTypeInfo="TRUE"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;Release\&quot;&quot;"
				AssemblerListingLocation="Release"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/Release/pcre_scanner_unittest.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories=".;..;"
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
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib pcrecpp.lib pcre.lib "
				OutputFile="Release\pcre_scanner_unittest.exe"
				Version="0.0"
				LinkIncremental="1"
				AdditionalLibraryDirectories=".\$(OutDir),."
				ProgramDataBaseFile="$(OutDir)\pcre_scanner_unittest.pdb"
				SubSystem="1"
				StackReserveSize="10000000"/>
		</Configuration>
		<Configuration
			Name="MinSizeRel|Win32"
			OutputDirectory="MinSizeRel"
			IntermediateDirectory="pcre_scanner_unittest.dir\MinSizeRel"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories=".;..;"
				CompileAs="2"
				ExceptionHandling="TRUE"
				InlineFunctionExpansion="1"
				Optimization="1"
				RuntimeLibrary="2"
				RuntimeTypeInfo="TRUE"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;MinSizeRel\&quot;&quot;"
				AssemblerListingLocation="MinSizeRel"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/MinSizeRel/pcre_scanner_unittest.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories=".;..;"
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
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib pcrecpp.lib pcre.lib "
				OutputFile="MinSizeRel\pcre_scanner_unittest.exe"
				Version="0.0"
				LinkIncremental="1"
				AdditionalLibraryDirectories=".\$(OutDir),."
				ProgramDataBaseFile="$(OutDir)\pcre_scanner_unittest.pdb"
				SubSystem="1"
				StackReserveSize="10000000"/>
		</Configuration>
		<Configuration
			Name="RelWithDebInfo|Win32"
			OutputDirectory="RelWithDebInfo"
			IntermediateDirectory="pcre_scanner_unittest.dir\RelWithDebInfo"
			ConfigurationType="1"
			UseOfMFC="0"
			ATLMinimizesCRunTimeLibraryUsage="FALSE"
			CharacterSet="2">
			<Tool
				Name="VCCLCompilerTool"
				AdditionalOptions=" /Zm1000"
				AdditionalIncludeDirectories=".;..;"
				CompileAs="2"
				DebugInformationFormat="3"
				ExceptionHandling="TRUE"
				InlineFunctionExpansion="1"
				Optimization="2"
				RuntimeLibrary="2"
				RuntimeTypeInfo="TRUE"
				WarningLevel="3"
				PreprocessorDefinitions="WIN32,_WINDOWS,NDEBUG,HAVE_CONFIG_H,_CRT_SECURE_NO_DEPRECATE,&quot;CMAKE_INTDIR=\&quot;RelWithDebInfo\&quot;&quot;"
				AssemblerListingLocation="RelWithDebInfo"
				ObjectFile="$(IntDir)\"
				ProgramDataBaseFileName="C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/win32/RelWithDebInfo/pcre_scanner_unittest.pdb"
/>
			<Tool
				Name="VCCustomBuildTool"/>
			<Tool
				Name="VCResourceCompilerTool"
				AdditionalIncludeDirectories=".;..;"
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
				AdditionalDependencies="$(NOINHERIT) kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib pcrecpp.lib pcre.lib "
				OutputFile="RelWithDebInfo\pcre_scanner_unittest.exe"
				Version="0.0"
				LinkIncremental="2"
				AdditionalLibraryDirectories=".\$(OutDir),."
				ProgramDataBaseFile="$(OutDir)\pcre_scanner_unittest.pdb"
				GenerateDebugInformation="TRUE"
				SubSystem="1"
				StackReserveSize="10000000"/>
		</Configuration>
	</Configurations>
	<Files>
			<File
				RelativePath="..\CMakeLists.txt">
				<FileConfiguration
					Name="Debug|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; -H.. -B."
					AdditionalDependencies="..\CMakeLists.txt;CMakeFiles\CMakeSystem.cmake;CMakeFiles\CMakeCCompiler.cmake;CMakeFiles\CMakeCXXCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;CMakeFiles\CMakeRCCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;..\config-cmake.h.in;..\pcre.h.generic;..\pcre_stringpiece.h.in;..\pcrecpparg.h.in;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;..\CMakeLists.txt;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
				<FileConfiguration
					Name="Release|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; -H.. -B."
					AdditionalDependencies="..\CMakeLists.txt;CMakeFiles\CMakeSystem.cmake;CMakeFiles\CMakeCCompiler.cmake;CMakeFiles\CMakeCXXCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;CMakeFiles\CMakeRCCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;..\config-cmake.h.in;..\pcre.h.generic;..\pcre_stringpiece.h.in;..\pcrecpparg.h.in;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;..\CMakeLists.txt;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
				<FileConfiguration
					Name="MinSizeRel|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; -H.. -B."
					AdditionalDependencies="..\CMakeLists.txt;CMakeFiles\CMakeSystem.cmake;CMakeFiles\CMakeCCompiler.cmake;CMakeFiles\CMakeCXXCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;CMakeFiles\CMakeRCCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;..\config-cmake.h.in;..\pcre.h.generic;..\pcre_stringpiece.h.in;..\pcrecpparg.h.in;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;..\CMakeLists.txt;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
				<FileConfiguration
					Name="RelWithDebInfo|Win32">
					<Tool
					Name="VCCustomBuildTool"
					Description="Building Custom Rule C:/Documents and Settings/kru028.NEXUS/Desktop/braccetto code/avis/support/pcre-7.4/CMakeLists.txt"
					CommandLine="&quot;C:\Program Files\CMake 2.4\bin\cmake.exe&quot; -H.. -B."
					AdditionalDependencies="..\CMakeLists.txt;CMakeFiles\CMakeSystem.cmake;CMakeFiles\CMakeCCompiler.cmake;CMakeFiles\CMakeCXXCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeSystemSpecificInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeGenericSystem.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\gcc.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;CMakeFiles\CMakeRCCompiler.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeRCInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCXXInformation.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\Windows-cl.cmake&quot;;CMakeFiles\CMakeCPlatform.cmake;CMakeFiles\CMakeCXXPlatform.cmake;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\Platform\WindowsPaths.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CMakeCommonLanguageInclude.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFile.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckIncludeFileCXX.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckFunctionExists.cmake&quot;;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Modules\CheckTypeSize.cmake&quot;;..\config-cmake.h.in;..\pcre.h.generic;..\pcre_stringpiece.h.in;..\pcrecpparg.h.in;&quot;C:\Program Files\CMake 2.4\share\cmake-2.4\Templates\CMakeWindowsSystemConfig.cmake&quot;;..\CMakeLists.txt;"
					Outputs="pcretest.vcproj.cmake"/>
				</FileConfiguration>
			</File>
		<Filter
			Name="Source Files"
			Filter="">
			<File
				RelativePath="..\pcre_scanner_unittest.cc">
			</File>
		</Filter>
	</Files>
	<Globals>
	</Globals>
</VisualStudioProject>
