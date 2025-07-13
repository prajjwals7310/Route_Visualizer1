Direction: ${direction}
Component: ${component}

${"Attribute"?right_pad(30)} ${"Default"?right_pad(20)} ${"Specification"?right_pad(30)}
------------------------------------------------------------

<#list attributes as attr>
${attr.name?right_pad(30)} ${attr.default?right_pad(20)} ${attr.spec?right_pad(30)}
</#list>