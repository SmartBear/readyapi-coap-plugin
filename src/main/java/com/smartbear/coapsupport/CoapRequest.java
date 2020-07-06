package com.smartbear.coapsupport;


import com.eviware.soapui.impl.wsdl.submit.transports.http.HttpResponse;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.settings.UISettings;
import com.smartbear.ready.core.ApplicationEnvironment;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import org.apache.xmlbeans.XmlObject;

import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.ImageIcon;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;


public class CoapRequest extends HttpTestRequest implements CoapOptionsDataSource {
    private static final String OPTION_SECTION = "Option";
    private static final String OPTION_NUMBER_ATTR = "Number";
    private static final String OPTION_VALUE_ATTR = "Value";
    private static final String CONFIRMABLE_ATTR = "confirmable";
    private static final String AUTO_SIZE1_ATTR = "autoSize1";
    public static final String CONTENT_FORMAT_OPTION_BEAN_PROP = "contentFormatOption";
    public static final String CONFIRMABLE_BEAN_PROP = "confirmable";
    public static final String RESPONSE_ETAG_BEAN_PROP = "responseEtag";
    public static final String RESPONSE_LOCATION_BEAN_PROP = "responseLocation";
    public static final String AUTO_SIZE1_BEAN_PROP = "autoSize1";

    private CoapRequestTestStep testStep;
    private ImageIcon unknownRequestIcon;
    private ImageIcon disabledRequestIcon;
    private boolean forLoadTest;
    private ArrayList<CoapOptionsListener> optionsListeners;
    private String contentFormatOption;

    public CoapRequest(CoapRequestTestStep testStep, HttpRequestConfig config, boolean forLoadTest) {
        super(config, testStep, forLoadTest);
        this.testStep = testStep;
        this.forLoadTest = forLoadTest;
        this.contentFormatOption = readContentFormatOption();
    }

    @Override
    protected void initIcons() {
        unknownRequestIcon = UISupport.createImageIcon("com/smartbear/coapsupport/coap_request_step.png");
        disabledRequestIcon = UISupport.createImageIcon("com/smartbear/coapsupport/disabled_coap_request_step.png");

        setIconAnimator(new RequestIconAnimator<CoapRequest>(this, "com/smartbear/coapsupport/coap_request.png", "com/smartbear/coapsupport/coap_request.png", 5));
    }

    @Override
    public ImageIcon getIcon() {
        if (forLoadTest || getIconAnimator() == null) {
            return null;
        }

        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null
                && (testMonitor.hasRunningLoadTest(getTestStep().getTestCase()) || testMonitor
                .hasRunningSecurityTest(getTestStep().getTestCase()))) {
            return disabledRequestIcon;
        }

        ImageIcon icon = getIconAnimator().getIcon();
        if (icon == getIconAnimator().getBaseIcon()) {
            boolean isColorBlindMode = ApplicationEnvironment.getSettings().getBoolean(UISettings.COLOR_BLIND_MODE);
            AssertionStatus status = getAssertionStatus();
            if (status == AssertionStatus.VALID) {
                if (isColorBlindMode) {
                    return UISupport.createImageIcon("com/smartbear/coapsupport/valid_coap_request_step_color_blind.png");
                } else {
                    return UISupport.createImageIcon("com/smartbear/coapsupport/valid_coap_request_step.png");
                }
            } else if (status == AssertionStatus.FAILED) {
                if (isColorBlindMode) {
                    return UISupport.createImageIcon("com/smartbear/coapsupport/invalid_coap_request_step_color_blind.png");
                } else {
                    return UISupport.createImageIcon("com/smartbear/coapsupport/invalid_coap_request_step.png");
                }
            } else if (status == AssertionStatus.UNKNOWN) {
                return unknownRequestIcon;
            }
        }

        return icon;
    }

    public boolean getConfirmable() {
        Element element = (Element) getConfig().getDomNode();
        String rawValue = element.getAttributeNS(null, CONFIRMABLE_ATTR);
        if (StringUtils.isNullOrEmpty(rawValue)) {
            return true;
        }
        return Boolean.valueOf(rawValue);

    }

    public void setConfirmable(boolean newValue) {
        boolean oldConfirmable = getConfirmable();
        if (newValue != oldConfirmable) {
            Element element = (Element) getConfig().getDomNode();
            element.setAttributeNS(null, CONFIRMABLE_ATTR, Boolean.toString(newValue));
            notifyPropertyChanged(CONFIRMABLE_BEAN_PROP, oldConfirmable, newValue);
        }
    }

    public boolean getAutoSize1() {
        Element element = (Element) getConfig().getDomNode();
        String rawValue = element.getAttributeNS(null, AUTO_SIZE1_ATTR);
        if (StringUtils.isNullOrEmpty(rawValue)) {
            return false;
        }
        return Boolean.valueOf(rawValue);
    }

    public void setAutoSize1(boolean newValue) {
        boolean oldValue = getAutoSize1();
        if (newValue != oldValue) {
            Element element = (Element) getConfig().getDomNode();
            element.setAttributeNS(null, AUTO_SIZE1_ATTR, Boolean.toString(newValue));
            notifyPropertyChanged(AUTO_SIZE1_BEAN_PROP, oldValue, newValue);
        }
    }

    public ModelItem getParent() {
        return testStep;
    }

    public void updateConfig(HttpRequestConfig request) {
        String oldContentFormat = contentFormatOption;
        super.updateConfig(request);
        contentFormatOption = readContentFormatOption();
        if (optionsListeners != null) {
            for (CoapOptionsListener listener : optionsListeners) {
                listener.onWholeOptionListChanged();
            }
        }
        if (Utils.areStringsEqual(oldContentFormat, contentFormatOption, true, false)) {
            contentFomatChanged(oldContentFormat);
        }
    }

    private static String getElementText(XmlObject obj) {
        return getElementText((Element) obj.getDomNode());
    }

    private static String getElementText(Element element) {
        String result = "";
        Node child = element.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                result += child.getNodeValue();
            }
            child = child.getNextSibling();
        }
        return result;
    }

    @Override
    public int getOptionCount() {
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        return optionSections.length;
    }

    private int readOptionNumber(XmlObject optionSection) {
        return readOptionNumber((Element) optionSection.getDomNode());
    }

    private int readOptionNumber(Element optionSection) {
        String optionNumberStr = optionSection.getAttribute(OPTION_NUMBER_ATTR);
        int optionNumber;
        try {
            optionNumber = Integer.parseInt(optionNumberStr);
        } catch (NumberFormatException e) {
            SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionNumberStr, getName()));
            return -1;
        }
        return optionNumber;
    }

    private String readOptionValue(Element optionSection) {
        return optionSection.getAttribute(OPTION_VALUE_ATTR);
    }

    private String readOptionValue(XmlObject optionSection) {
        return readOptionValue((Element) optionSection.getDomNode());
    }

    private Element getOptionSection(int optionIndex) {
        String path = String.format("$this/%s[%d]", OPTION_SECTION, optionIndex + 1);
        XmlObject[] pathResult = getConfig().selectPath(path);
        if (pathResult == null || pathResult.length == 0) {
            return null;
        }
        return (Element) pathResult[0].getDomNode();
    }

    @Override
    public int getOptionNumber(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        return readOptionNumber(optionSection);
    }

    @NotNull
    @Override
    public String getOptionValue(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        return readOptionValue(optionSection);
    }

    @Override
    public void setOption(int optionIndex, String optionValue) {
        if (optionValue == null) {
            optionValue = "";
        }
        Element optionSection = getOptionSection(optionIndex);

        String oldOptionValue = readOptionValue(optionSection);
        if (oldOptionValue == null) {
            oldOptionValue = "";
        }
        if (Utils.areStringsEqual(oldOptionValue, optionValue, false, true)) {
            return;
        }

        int optionNumber = readOptionNumber(optionSection);
        if (optionValue == null || optionValue.length() == 0) {
            optionSection.removeAttribute(OPTION_VALUE_ATTR);
        } else {
            optionSection.setAttribute(OPTION_VALUE_ATTR, optionValue);
        }
        if (optionNumber == OptionNumberRegistry.CONTENT_FORMAT) {
            contentFormatOption = optionValue;
        }
        if (optionsListeners != null) {
            for (CoapOptionsListener listener : optionsListeners) {
                listener.onOptionChanged(optionIndex, optionNumber, optionNumber, oldOptionValue, optionValue);
            }
        }
        if (optionNumber == OptionNumberRegistry.CONTENT_FORMAT) {
            contentFomatChanged(oldOptionValue);
        }
    }

    @Override
    public int addOption(int optionNumber, String optionValue) {
        if (optionValue == null) {
            optionValue = "";
        }

        Element newOptionSection = getConfig().getDomNode().getOwnerDocument().createElement(OPTION_SECTION);
        newOptionSection.setAttribute(OPTION_NUMBER_ATTR, Integer.toString(optionNumber));
        if (optionValue.length() != 0) {
            newOptionSection.setAttribute(OPTION_VALUE_ATTR, optionValue);
        }

        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        int pos;
        Element optionSection = null;
        for (pos = 0; pos < optionSections.length; ++pos) {
            optionSection = (Element) optionSections[pos].getDomNode();
            int curOptionNumber = readOptionNumber(optionSection);
            if (optionNumber > curOptionNumber) {
                break;
            }
        }
        if (pos == optionSections.length) {
            getConfig().getDomNode().appendChild(newOptionSection);
        } else {
            getConfig().getDomNode().insertBefore(newOptionSection, optionSection);
        }
        if (optionNumber == OptionNumberRegistry.CONTENT_FORMAT) {
            contentFormatOption = optionValue;
        }
        if (optionsListeners != null) {
            for (CoapOptionsListener listener : optionsListeners) {
                listener.onOptionAdded(pos, optionNumber, optionValue);
            }
        }
        if (optionNumber == OptionNumberRegistry.CONTENT_FORMAT) {
            contentFomatChanged(null);
        }
        return pos;
    }

    @Override
    public void removeOption(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        String oldOption = readOptionValue(optionSection);
        if (oldOption == null) {
            oldOption = "";
        }

        int oldOptionNumber = readOptionNumber(optionSection);
        getConfig().getDomNode().removeChild(optionSection);
        if (oldOptionNumber == OptionNumberRegistry.CONTENT_FORMAT) {
            contentFormatOption = null;
        }
        if (optionsListeners != null) {
            for (CoapOptionsListener listener : optionsListeners) {
                listener.onOptionRemoved(optionIndex, oldOptionNumber, oldOption);
            }
        }
        if (oldOptionNumber == OptionNumberRegistry.CONTENT_FORMAT) {
            contentFomatChanged(oldOption);
        }
    }


    @Override
    public void moveOption(int optionIndex, int delta) {
        if (delta == 0) {
            return;
        }
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        int optionNumber = readOptionNumber(optionSections[optionIndex]);
        String optionValue = readOptionValue(optionSections[optionIndex]);
        Node movedSection = optionSections[optionIndex].getDomNode();
        getConfig().getDomNode().removeChild(movedSection);
        if (optionsListeners != null) {
            for (CoapOptionsListener listener : optionsListeners) {
                listener.onOptionRemoved(optionIndex, optionNumber, optionValue);
            }
        }
        int newIndex = optionIndex + delta;
        if (newIndex == optionSections.length - 1) {
            getConfig().getDomNode().appendChild(movedSection);
        } else {
            if (delta > 0) {
                getConfig().getDomNode().insertBefore(movedSection, optionSections[newIndex + 1].getDomNode());
            } else {
                getConfig().getDomNode().insertBefore(movedSection, optionSections[newIndex].getDomNode());
            }
        }
        if (optionsListeners != null) {
            for (CoapOptionsListener listener : optionsListeners) {
                listener.onOptionAdded(optionIndex, optionNumber, optionValue);
            }
        }
    }

    private static String getMediaTypeString(String optionValue) {
        Long mediaType = OptionsSupport.decodeIntOptionValue(optionValue, 2);
        if (mediaType == null || mediaType == MediaTypeRegistry.UNDEFINED) {
            return "";
        }
        for (int it : MediaTypeRegistry.getAllMediaTypes()) {
            if (mediaType == it) {
                return MediaTypeRegistry.toString(mediaType.intValue());
            }
        }
        return "";

    }

    public Integer getContentFormatAsNumber() {
        Long mediaType = OptionsSupport.decodeIntOptionValue(getContentFormatOption(), 2);
        if (mediaType == null) {
            return null;
        } else {
            return mediaType.intValue();
        }
    }

    @Override
    public String getMediaType() {
        return getMediaTypeString(getContentFormatOption());
    }

    private void contentFomatChanged(String oldValue) {
        notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldValue, contentFormatOption);
        String oldMediaType = getMediaTypeString(oldValue);
        String newMediaType = getMediaTypeString(contentFormatOption);
        if (Utils.areStringsEqual(oldMediaType, newMediaType, true, true)) {
            notifyPropertyChanged(MEDIA_TYPE, oldMediaType, newMediaType);
        }
    }

    @Override
    public void setMediaType(String mediaType) {
        if (StringUtils.isNullOrEmpty(mediaType)) {
            return;
        }
        int number = MediaTypeRegistry.parse(mediaType);
        if (number == MediaTypeRegistry.UNDEFINED) {
            return;
        }
        setContentFormatOption(OptionsSupport.encodeIntOptionValue(number, 2));
    }

    private String readContentFormatOption() {
        XmlObject[] mediaTypeSections = getConfig().selectPath(String.format("$this/%s[@%s=%d]", OPTION_SECTION, OPTION_NUMBER_ATTR, OptionNumberRegistry.CONTENT_FORMAT));
        if (mediaTypeSections == null || mediaTypeSections.length == 0) {
            return null;
        }
        if (mediaTypeSections.length != 1) {
            SoapUI.log(String.format("Incorrect data (duplicated Content-Format option) in the %s test step", getName()));
        }
        return readOptionValue(mediaTypeSections[0]);
    }

    public String getContentFormatOption() {
        return contentFormatOption;
    }

    public void setContentFormatOption(String value) {
        String oldValue = getContentFormatOption();
        if (Utils.areStringsEqual(oldValue, value, true, false)) {
            return;
        }
        int count = getOptionCount();
        for (int i = 0; i < count; ++i) {
            if (getOptionNumber(i) == OptionNumberRegistry.CONTENT_FORMAT) {
                if (value == null) {
                    removeOption(i);
                } else {
                    setOption(i, value);
                }
                return;
            }
        }
        addOption(OptionNumberRegistry.CONTENT_FORMAT, value);
    }

    public void addOptionsListener(CoapOptionsListener listener) {
        if (optionsListeners == null) {
            optionsListeners = new ArrayList<>();
        }
        optionsListeners.add(listener);
    }

    public void removeOptionsListener(CoapOptionsListener listener) {
        if (optionsListeners == null) {
            return;
        }
        optionsListeners.remove(listener);
    }

    public void setResponse(HttpResponse response, SubmitContext context) {
        String oldEtag = getResponseEtag();
        String oldResponseLocation = getResponseLocation();
        super.setResponse(response, context);
        String newETag = getResponseEtag();
        String newResponseLocation = getResponseLocation();
        if (!Utils.areStringsEqual(oldEtag, newETag, true, true)) {
            notifyPropertyChanged(RESPONSE_ETAG_BEAN_PROP, oldEtag, newETag);
        }
        if (!Utils.areStringsEqual(oldResponseLocation, newResponseLocation, false, true)) {
            notifyPropertyChanged(RESPONSE_LOCATION_BEAN_PROP, oldResponseLocation, newResponseLocation);
        }
    }

    public String getResponseEtag() {
        CoapResp response = (CoapResp) getResponse();
        if (response == null) {
            return null;
        }
        if (response.getResponseMessage() == null) {
            return null;
        }
        List<byte[]> etags = response.getResponseMessage().getOptions().getETags();
        if (etags == null || etags.size() == 0) {
            return null;
        }
        String result = "";
        for (byte[] etag : etags) {
            String s = Utils.bytesToHexString(etag);
            if (s != null) {
                result += ("0x" + s);
            }
            result += ";";
        }
        return result.substring(0, result.length() - 1);
    }

    public String getResponseLocation() {
        CoapResp response = (CoapResp) getResponse();
        if (response == null) {
            return null;
        }
        if (response.getResponseMessage() == null) {
            return null;
        }
        return response.getResponseMessage().getOptions().getLocationString();

    }
}