<#if makeSrcAndSimulationsFolders>
<@setoutput path=simulationsFolder+"/package.ned"/>
<#if simulationsPackage!="">
// designate the NED package of this folder
package ${simulationsPackage};

</#if>

<#if licenseCode!="">
@license(${licenseCode});
</#if>
</#if>

