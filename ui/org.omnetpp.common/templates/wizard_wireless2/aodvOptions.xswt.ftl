<?xml version="1.0" encoding="UTF-8"?>
<xswt xmlns:x="http://sweet_swt.sf.net/xswt">

  <import xmlns="http://sweet_swt.sf.net/xswt">
    <package name="java.lang"/>
    <package name="org.eclipse.swt.widgets" />
    <package name="org.eclipse.swt.graphics" />
    <package name="org.eclipse.swt.layout" />
    <package name="org.omnetpp.common.wizard.support" />
    <package name="org.omnetpp.ned.editor.wizards.support" />
    <package name="org.omnetpp.cdt.wizard.support" />
  </import>
  <layout x:class="GridLayout" numColumns="2"/>
  <x:children>
    <group text="Configure AODV options:">
        <layout x:class="GridLayout" numColumns="2"/>
        <layoutData x:class="GridData" horizontalAlignment="FILL" grabExcessHorizontalSpace="true"/>
        <x:children>
            <label text="Dummy AODV option:"/>
            <text x:style="BORDER"/>
            <label text="Dummy AODV option 2:"/>
            <text x:style="BORDER"/>
            <label text="Dummy AODV option 3:"/>
            <text x:style="BORDER"/>
        </x:children>
    </group>
  </x:children>
</xswt>
