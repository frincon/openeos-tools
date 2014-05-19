<#-- // Fields -->
<#macro toUnderScore camelCase>
${camelCase?replace("[A-Z]","_$0",'r')?upper_case}</#macro> 

<#foreach field in pojo.getAllPropertiesIterator()><#if pojo.getMetaAttribAsBool(field, "gen-property", true)> <#if pojo.hasMetaAttribute(field, "field-description")>    /**
     ${pojo.getFieldJavaDoc(field, 0)}
     */
 </#if>    public static final String PROPERTY_<@toUnderScore camelCase=field.name/> = "${field.name}";
</#if>
</#foreach>
