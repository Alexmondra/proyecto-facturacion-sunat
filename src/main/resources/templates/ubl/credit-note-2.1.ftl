<?xml version="1.0" encoding="UTF-8"?>
<CreditNote xmlns="urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2"
            xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
            xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
            xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
            xmlns:ext="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2"
            xmlns:qdt="urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2"
            xmlns:udt="urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2"
            xmlns:ccts="urn:un:unece:uncefact:documentation:2">
    <ext:UBLExtensions>
        <ext:UBLExtension>
            <ext:ExtensionContent>
            </ext:ExtensionContent>
        </ext:UBLExtension>
    </ext:UBLExtensions>
    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>
    <cbc:CustomizationID>2.0</cbc:CustomizationID>
    <cbc:ID>${idComprobante}</cbc:ID>
    <cbc:IssueDate>${fechaEmision}</cbc:IssueDate>
    <cbc:IssueTime>${horaEmision}</cbc:IssueTime>
    <#if leyendaMontoLetras?? && leyendaMontoLetras?has_content>
    <cbc:Note languageLocaleID="1000"><![CDATA[${leyendaMontoLetras}]]></cbc:Note>
    </#if>
    <cbc:DocumentCurrencyCode listAgencyName="United Nations Economic Commission for Europe" listID="ISO 4217 Alpha" listName="Currency">${moneda}</cbc:DocumentCurrencyCode>
    <#list referencias as ref>
    <cac:DiscrepancyResponse>
        <cbc:ReferenceID>${ref.serieRef}-${ref.numeroRef}</cbc:ReferenceID>
        <cbc:ResponseCode listAgencyName="PE:SUNAT" listName="Tipo de nota de credito" listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo09">${ref.motivoCodigo}</cbc:ResponseCode>
        <cbc:Description><![CDATA[${ref.motivoDescripcion}]]></cbc:Description>
    </cac:DiscrepancyResponse>
    <cac:BillingReference>
        <cac:InvoiceDocumentReference>
            <cbc:ID>${ref.serieRef}-${ref.numeroRef}</cbc:ID>
            <cbc:DocumentTypeCode listAgencyName="PE:SUNAT" listName="Tipo de Documento" listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01">${ref.tipoDocumentoRef}</cbc:DocumentTypeCode>
        </cac:InvoiceDocumentReference>
    </cac:BillingReference>
    </#list>
    <cac:Signature>
        <cbc:ID>${emisorRuc}</cbc:ID>
        <cac:SignatoryParty>
            <cac:PartyIdentification>
                <cbc:ID>${emisorRuc}</cbc:ID>
            </cac:PartyIdentification>
            <cac:PartyName>
                <cbc:Name><![CDATA[${emisorRazonSocial}]]></cbc:Name>
            </cac:PartyName>
        </cac:SignatoryParty>
        <cac:DigitalSignatureAttachment>
            <cac:ExternalReference>
                <cbc:URI>#SignSUNAT</cbc:URI>
            </cac:ExternalReference>
        </cac:DigitalSignatureAttachment>
    </cac:Signature>
    <cac:AccountingSupplierParty>
        <cac:Party>
            <cac:PartyIdentification>
                <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="6" schemeName="Documento de Identidad" schemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06">${emisorRuc}</cbc:ID>
            </cac:PartyIdentification>
            <cac:PartyName>
                <cbc:Name><![CDATA[${emisorRazonSocial}]]></cbc:Name>
            </cac:PartyName>
            <cac:PartyLegalEntity>
                <cbc:RegistrationName><![CDATA[${emisorRazonSocial}]]></cbc:RegistrationName>
                <cac:RegistrationAddress>
                    <cbc:AddressTypeCode listAgencyName="PE:SUNAT" listName="Establecimientos anexos">${codigoSucursal!"0000"}</cbc:AddressTypeCode>
                    <#if ubigeoSucursal?? && ubigeoSucursal?has_content>
                    <cbc:District>${ubigeoSucursal}</cbc:District>
                    </#if>
                    <#if direccionFiscal?? && direccionFiscal?has_content>
                    <cac:AddressLine>
                        <cbc:Line><![CDATA[${direccionFiscal}]]></cbc:Line>
                    </cac:AddressLine>
                    </#if>
                </cac:RegistrationAddress>
            </cac:PartyLegalEntity>
        </cac:Party>
    </cac:AccountingSupplierParty>
    <cac:AccountingCustomerParty>
        <cac:Party>
            <cac:PartyIdentification>
                <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="${clienteTipoDoc}" schemeName="Documento de Identidad" schemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06">${clienteNumeroDoc}</cbc:ID>
            </cac:PartyIdentification>
            <cac:PartyLegalEntity>
                <cbc:RegistrationName><![CDATA[${clienteNombre}]]></cbc:RegistrationName>
                <#if clienteDireccion?? && clienteDireccion?has_content>
                <cac:RegistrationAddress>
                    <cac:AddressLine>
                        <cbc:Line><![CDATA[${clienteDireccion}]]></cbc:Line>
                    </cac:AddressLine>
                </cac:RegistrationAddress>
                </#if>
            </cac:PartyLegalEntity>
        </cac:Party>
    </cac:AccountingCustomerParty>
    <cac:TaxTotal>
        <cbc:TaxAmount currencyID="${moneda}">${(totalTributos!0.00)?string["0.00"]}</cbc:TaxAmount>
        <#if tributosGlobales?? && (tributosGlobales?size > 0)>
        <#list tributosGlobales as tg>
        <cac:TaxSubtotal>
            <#if tg.esIcbper?? && tg.esIcbper>
            <cbc:TaxAmount currencyID="${moneda}">${tg.monto?string["0.00"]}</cbc:TaxAmount>
            <cac:TaxCategory>
                <cac:TaxScheme>
                    <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="UN/ECE 5153" schemeName="Codigo de tributos">7152</cbc:ID>
                    <cbc:Name>ICBPER</cbc:Name>
                    <cbc:TaxTypeCode>OTH</cbc:TaxTypeCode>
                </cac:TaxScheme>
            </cac:TaxCategory>
            <#else>
            <cbc:TaxableAmount currencyID="${moneda}">${tg.baseImponible?string["0.00"]}</cbc:TaxableAmount>
            <cbc:TaxAmount currencyID="${moneda}">${tg.monto?string["0.00"]}</cbc:TaxAmount>
            <cac:TaxCategory>
                <cac:TaxScheme>
                    <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="UN/ECE 5153" schemeName="Codigo de tributos">${tg.codigoTributo}</cbc:ID>
                    <cbc:Name>${tg.nombreTributo}</cbc:Name>
                    <cbc:TaxTypeCode>${tg.codigoInternacional}</cbc:TaxTypeCode>
                </cac:TaxScheme>
            </cac:TaxCategory>
            </#if>
        </cac:TaxSubtotal>
        </#list>
        <#else>
        <cac:TaxSubtotal>
            <cbc:TaxableAmount currencyID="${moneda}">${subtotal?string["0.00"]}</cbc:TaxableAmount>
            <cbc:TaxAmount currencyID="${moneda}">${(totalTributos!0.00)?string["0.00"]}</cbc:TaxAmount>
            <cac:TaxCategory>
                <cac:TaxScheme>
                    <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="UN/ECE 5153" schemeName="Codigo de tributos">1000</cbc:ID>
                    <cbc:Name>IGV</cbc:Name>
                    <cbc:TaxTypeCode>VAT</cbc:TaxTypeCode>
                </cac:TaxScheme>
            </cac:TaxCategory>
        </cac:TaxSubtotal>
        </#if>
    </cac:TaxTotal>
    <cac:LegalMonetaryTotal>
        <cbc:LineExtensionAmount currencyID="${moneda}">${subtotal?string["0.00"]}</cbc:LineExtensionAmount>
        <cbc:TaxInclusiveAmount currencyID="${moneda}">${total?string["0.00"]}</cbc:TaxInclusiveAmount>
        <#if totalDescuentos?? && (totalDescuentos > 0)>
        <cbc:AllowanceTotalAmount currencyID="${moneda}">${totalDescuentos?string["0.00"]}</cbc:AllowanceTotalAmount>
        </#if>
        <#if totalOtrosCargos?? && (totalOtrosCargos > 0)>
        <cbc:ChargeTotalAmount currencyID="${moneda}">${totalOtrosCargos?string["0.00"]}</cbc:ChargeTotalAmount>
        </#if>
        <cbc:PayableAmount currencyID="${moneda}">${total?string["0.00"]}</cbc:PayableAmount>
    </cac:LegalMonetaryTotal>
    <#list lineas as linea>
    <cac:CreditNoteLine>
        <cbc:ID>${linea.item}</cbc:ID>
        <cbc:CreditedQuantity unitCode="${linea.unidadMedida}" unitCodeListAgencyName="United Nations Economic Commission for Europe" unitCodeListID="UN/ECE rec 20">${linea.cantidad?string["0.00"]}</cbc:CreditedQuantity>
        <cbc:LineExtensionAmount currencyID="${moneda}">${linea.valorVenta?string["0.00"]}</cbc:LineExtensionAmount>
        <cac:PricingReference>
            <cac:AlternativeConditionPrice>
                <cbc:PriceAmount currencyID="${moneda}">${linea.precioReferencial?string["0.00"]}</cbc:PriceAmount>
                <cbc:PriceTypeCode listAgencyName="PE:SUNAT" listName="Tipo de Precio" listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo16">${linea.tipoPrecioReferencial}</cbc:PriceTypeCode>
            </cac:AlternativeConditionPrice>
        </cac:PricingReference>
        <cac:TaxTotal>
            <cbc:TaxAmount currencyID="${moneda}">${(linea.totalTributos!0.00)?string["0.00"]}</cbc:TaxAmount>
            <#list linea.tributos as trib>
            <cac:TaxSubtotal>
                <#if trib.esIcbper?? && trib.esIcbper>
                <cbc:TaxAmount currencyID="${moneda}">${trib.monto?string["0.00"]}</cbc:TaxAmount>
                <cbc:BaseUnitMeasure unitCode="NIU">${trib.cantidadBase?string["0"]}</cbc:BaseUnitMeasure>
                <cac:TaxCategory>
                    <cbc:PerUnitAmount currencyID="${moneda}">${trib.montoUnitario?string["0.00"]}</cbc:PerUnitAmount>
                    <cac:TaxScheme>
                        <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="UN/ECE 5153" schemeName="Codigo de tributos">7152</cbc:ID>
                        <cbc:Name>ICBPER</cbc:Name>
                        <cbc:TaxTypeCode>OTH</cbc:TaxTypeCode>
                    </cac:TaxScheme>
                </cac:TaxCategory>
                <#else>
                <cbc:TaxableAmount currencyID="${moneda}">${trib.baseImponible?string["0.00"]}</cbc:TaxableAmount>
                <cbc:TaxAmount currencyID="${moneda}">${trib.monto?string["0.00"]}</cbc:TaxAmount>
                <cac:TaxCategory>
                    <cbc:Percent>${trib.porcentaje?string["0.00"]}</cbc:Percent>
                    <cbc:TaxExemptionReasonCode listAgencyName="PE:SUNAT" listName="Afectacion del IGV" listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo07">${linea.tipoAfectacionCodigo}</cbc:TaxExemptionReasonCode>
                    <cac:TaxScheme>
                        <cbc:ID schemeAgencyName="PE:SUNAT" schemeID="UN/ECE 5153" schemeName="Codigo de tributos">${trib.codigoTributo}</cbc:ID>
                        <cbc:Name>${trib.nombreTributo}</cbc:Name>
                        <cbc:TaxTypeCode>${trib.codigoInternacional}</cbc:TaxTypeCode>
                    </cac:TaxScheme>
                </cac:TaxCategory>
                </#if>
            </cac:TaxSubtotal>
            </#list>
        </cac:TaxTotal>
        <cac:Item>
            <cbc:Description><![CDATA[${linea.descripcion}]]></cbc:Description>
            <#if linea.codigoProducto?? && linea.codigoProducto != "-" && linea.codigoProducto?has_content>
            <cac:SellersItemIdentification>
                <cbc:ID><![CDATA[${linea.codigoProducto}]]></cbc:ID>
            </cac:SellersItemIdentification>
            </#if>
        </cac:Item>
        <cac:Price>
            <cbc:PriceAmount currencyID="${moneda}">${linea.valorUnitario?string["0.0000"]}</cbc:PriceAmount>
        </cac:Price>
    </cac:CreditNoteLine>
    </#list>
</CreditNote>
