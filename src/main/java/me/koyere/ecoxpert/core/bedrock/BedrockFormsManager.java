package me.koyere.ecoxpert.core.bedrock;

import me.koyere.ecoxpert.EcoXpertPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Bedrock Forms Manager - Geyser Forms API integration
 *
 * Uses reflection to support Geyser Forms without compile-time dependency.
 * Only activates when Geyser is detected on the server.
 *
 * Supports:
 * - SimpleForm (menu with buttons)
 * - ModalForm (yes/no confirmation)
 * - CustomForm (complex forms with inputs)
 */
public class BedrockFormsManager {

    private final EcoXpertPlugin plugin;
    private boolean geyserAvailable = false;
    private boolean formsApiAvailable = false;

    // Reflection handles
    private Class<?> geyserApiClass;
    private ClassLoader geyserClassLoader;
    private Method geyserGetApi;
    private Class<?> simpleFormClass;
    private Class<?> simpleFormBuilderClass;
    private Class<?> modalFormClass;
    private Class<?> modalFormBuilderClass;
    private Object geyserApiInstance;

    // Form callbacks
    private final ConcurrentHashMap<UUID, FormCallback> pendingCallbacks = new ConcurrentHashMap<>();

    public BedrockFormsManager(EcoXpertPlugin plugin) {
        this.plugin = plugin;
        detectGeyserForms();
    }

    /**
     * Detect Geyser Forms API availability using reflection
     */
    private void detectGeyserForms() {
        try {
            // Try to load Geyser API classes
            this.geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");

            // Get GeyserApi instance
            this.geyserGetApi = geyserApiClass.getMethod("api");
            this.geyserApiInstance = geyserGetApi.invoke(null);
            this.geyserClassLoader = geyserApiInstance.getClass().getClassLoader();

            // Load form classes
            this.simpleFormClass = loadGeyserClass("org.geysermc.cumulus.form.SimpleForm");
            this.simpleFormBuilderClass = loadGeyserClass("org.geysermc.cumulus.form.SimpleForm$Builder");
            this.modalFormClass = loadGeyserClass("org.geysermc.cumulus.form.ModalForm");
            this.modalFormBuilderClass = loadGeyserClass("org.geysermc.cumulus.form.ModalForm$Builder");

            this.geyserAvailable = true;
            this.formsApiAvailable = true;

            plugin.getLogger().info("Geyser Forms API detected - Bedrock-native UIs enabled");

        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Geyser Forms API not available - using fallback chest GUIs for Bedrock players");
            this.geyserAvailable = false;
            this.formsApiAvailable = false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading Geyser Forms API", e);
            this.geyserAvailable = false;
            this.formsApiAvailable = false;
        }
    }

    /**
     * Check if Geyser Forms are available
     */
    public boolean isFormsAvailable() {
        return formsApiAvailable;
    }

    /**
     * Send a SimpleForm (menu with buttons) to a player
     *
     * @param player Target player
     * @param title Form title
     * @param content Form description/content
     * @param buttons List of button labels
     * @param callback Callback when button clicked (receives button index)
     */
    public void sendSimpleForm(Player player, String title, String content,
                               List<String> buttons, Consumer<Integer> callback) {
        if (!formsApiAvailable) {
            plugin.getLogger().warning("Cannot send SimpleForm - Geyser Forms API not available");
            return;
        }

        try {
            // Create builder: SimpleForm.builder()
            Method builderMethod = simpleFormClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);

            // Set title: builder.title(title)
            Method titleMethod = simpleFormBuilderClass.getMethod("title", String.class);
            builder = titleMethod.invoke(builder, title);

            // Set content: builder.content(content)
            Method contentMethod = simpleFormBuilderClass.getMethod("content", String.class);
            builder = contentMethod.invoke(builder, content);

            // Add buttons: builder.button(label)
            Method buttonMethod = simpleFormBuilderClass.getMethod("button", String.class);
            for (String buttonLabel : buttons) {
                builder = buttonMethod.invoke(builder, buttonLabel);
            }

            // Set callback handler
            if (callback != null) {
                // Create response handler using reflection
                Class<?> formResponseClass = loadGeyserClass("org.geysermc.cumulus.response.SimpleFormResponse");
                Class<?> consumerClass = java.util.function.Consumer.class;

                // Create consumer that handles the response
                Consumer<Object> responseHandler = response -> {
                    try {
                        // Check if form was closed: response.isClosed()
                        Method isClosedMethod = formResponseClass.getMethod("isClosed");
                        Boolean closed = (Boolean) isClosedMethod.invoke(response);

                        if (!closed) {
                            // Get clicked button index: response.clickedButtonId()
                            Method clickedButtonMethod = formResponseClass.getMethod("clickedButtonId");
                            Integer buttonIndex = (Integer) clickedButtonMethod.invoke(response);

                            // Run callback on main thread
                            plugin.getServer().getScheduler().runTask(plugin,
                                () -> callback.accept(buttonIndex));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error handling SimpleForm response", e);
                    }
                };

                // Set valid result handler: builder.validResultHandler(handler)
                Method validResultMethod = simpleFormBuilderClass.getMethod("validResultHandler", consumerClass);
                builder = validResultMethod.invoke(builder, responseHandler);
            }

            // Build form: builder.build()
            Method buildMethod = simpleFormBuilderClass.getMethod("build");
            Object form = buildMethod.invoke(builder);

            // Send form to player via connection: GeyserApi.api().connectionByUuid(uuid).sendForm(form)
            if (!dispatchForm(player, form)) {
                plugin.getLogger().warning("Failed to send SimpleForm to " + player.getName() + " - no compatible sendForm method found");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error sending SimpleForm to " + player.getName(), e);
        }
    }

    /**
     * Send a ModalForm (yes/no confirmation) to a player
     *
     * @param player Target player
     * @param title Form title
     * @param content Form question/content
     * @param button1 First button text (typically "Yes")
     * @param button2 Second button text (typically "No")
     * @param callback Callback when button clicked (true = button1, false = button2)
     */
    public void sendModalForm(Player player, String title, String content,
                              String button1, String button2, Consumer<Boolean> callback) {
        if (!formsApiAvailable) {
            plugin.getLogger().warning("Cannot send ModalForm - Geyser Forms API not available");
            return;
        }

        try {
            // Create builder: ModalForm.builder()
            Method builderMethod = modalFormClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);

            // Set title: builder.title(title)
            Method titleMethod = modalFormBuilderClass.getMethod("title", String.class);
            builder = titleMethod.invoke(builder, title);

            // Set content: builder.content(content)
            Method contentMethod = modalFormBuilderClass.getMethod("content", String.class);
            builder = contentMethod.invoke(builder, content);

            // Set button1: builder.button1(text)
            Method button1Method = modalFormBuilderClass.getMethod("button1", String.class);
            builder = button1Method.invoke(builder, button1);

            // Set button2: builder.button2(text)
            Method button2Method = modalFormBuilderClass.getMethod("button2", String.class);
            builder = button2Method.invoke(builder, button2);

            // Set callback handler
            if (callback != null) {
                Class<?> formResponseClass = loadGeyserClass("org.geysermc.cumulus.response.ModalFormResponse");
                Class<?> consumerClass = java.util.function.Consumer.class;

                Consumer<Object> responseHandler = response -> {
                    try {
                        // Check if form was closed
                        Method isClosedMethod = formResponseClass.getMethod("isClosed");
                        Boolean closed = (Boolean) isClosedMethod.invoke(response);

                        if (!closed) {
                            // Get clicked button: response.clickedButtonId()
                            Method clickedButtonMethod = formResponseClass.getMethod("clickedButtonId");
                            Integer buttonId = (Integer) clickedButtonMethod.invoke(response);

                            // Run callback on main thread (0 = button1, 1 = button2)
                            plugin.getServer().getScheduler().runTask(plugin,
                                () -> callback.accept(buttonId == 0));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error handling ModalForm response", e);
                    }
                };

                // Set valid result handler
                Method validResultMethod = modalFormBuilderClass.getMethod("validResultHandler", consumerClass);
                builder = validResultMethod.invoke(builder, responseHandler);
            }

            // Build and send form
            Method buildMethod = modalFormBuilderClass.getMethod("build");
            Object form = buildMethod.invoke(builder);

            // Send form via connection
            if (!dispatchForm(player, form)) {
                plugin.getLogger().warning("Failed to send ModalForm to " + player.getName() + " - no compatible sendForm method found");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error sending ModalForm to " + player.getName(), e);
        }
    }

    /**
     * Builder class for creating SimpleForm configurations
     */
    public static class SimpleFormBuilder {
        private String title = "";
        private String content = "";
        private final List<String> buttons = new ArrayList<>();
        private Consumer<Integer> callback;

        public SimpleFormBuilder title(String title) {
            this.title = title;
            return this;
        }

        public SimpleFormBuilder content(String content) {
            this.content = content;
            return this;
        }

        public SimpleFormBuilder button(String label) {
            this.buttons.add(label);
            return this;
        }

        public SimpleFormBuilder callback(Consumer<Integer> callback) {
            this.callback = callback;
            return this;
        }

        public void sendTo(Player player, BedrockFormsManager manager) {
            manager.sendSimpleForm(player, title, content, buttons, callback);
        }
    }

    /**
     * Builder class for creating ModalForm configurations
     */
    public static class ModalFormBuilder {
        private String title = "";
        private String content = "";
        private String button1 = "Yes";
        private String button2 = "No";
        private Consumer<Boolean> callback;

        public ModalFormBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ModalFormBuilder content(String content) {
            this.content = content;
            return this;
        }

        public ModalFormBuilder button1(String button1) {
            this.button1 = button1;
            return this;
        }

        public ModalFormBuilder button2(String button2) {
            this.button2 = button2;
            return this;
        }

        public ModalFormBuilder callback(Consumer<Boolean> callback) {
            this.callback = callback;
            return this;
        }

        public void sendTo(Player player, BedrockFormsManager manager) {
            manager.sendModalForm(player, title, content, button1, button2, callback);
        }
    }

    /**
     * Create a new SimpleForm builder
     */
    public SimpleFormBuilder simpleForm() {
        return new SimpleFormBuilder();
    }

    /**
     * Create a new ModalForm builder
     */
    public ModalFormBuilder modalForm() {
        return new ModalFormBuilder();
    }

    /**
     * Send a CustomForm (form with inputs) to a player
     *
     * @param player Target player
     * @param title Form title
     * @param callback Callback when form submitted (receives Map of field results)
     * @param builder Configuration builder for form fields
     */
    public void sendCustomForm(Player player, String title,
                               java.util.function.Consumer<java.util.Map<String, Object>> callback,
                               java.util.function.Consumer<CustomFormBuilder> builder) {
        if (!formsApiAvailable) {
            plugin.getLogger().warning("Cannot send CustomForm - Geyser Forms API not available");
            return;
        }

        try {
            CustomFormBuilder formBuilder = new CustomFormBuilder(this, title);
            builder.accept(formBuilder);
            formBuilder.sendTo(player, this, callback);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error sending CustomForm to " + player.getName(), e);
        }
    }

    /**
     * Builder class for creating CustomForm configurations
     */
    public static class CustomFormBuilder {
        private final String title;
        private final BedrockFormsManager manager;
        private final java.util.List<FormComponent> components = new java.util.ArrayList<>();

        public CustomFormBuilder(BedrockFormsManager manager, String title) {
            this.title = title;
            this.manager = manager;
        }

        public CustomFormBuilder label(String text) {
            components.add(new FormComponent("label", text, null, null));
            return this;
        }

        public CustomFormBuilder input(String fieldName, String placeholder, String defaultValue) {
            components.add(new FormComponent("input", fieldName, placeholder, defaultValue));
            return this;
        }

        public CustomFormBuilder dropdown(String fieldName, java.util.List<String> options, int defaultIndex) {
            components.add(new FormComponent("dropdown", fieldName, options, defaultIndex));
            return this;
        }

        public CustomFormBuilder toggle(String fieldName, boolean defaultValue) {
            components.add(new FormComponent("toggle", fieldName, null, defaultValue));
            return this;
        }

        public CustomFormBuilder slider(String fieldName, float min, float max, float step, float defaultValue) {
            components.add(new FormComponent("slider", fieldName,
                java.util.Arrays.asList(min, max, step), defaultValue));
            return this;
        }

        void sendTo(Player player, BedrockFormsManager manager,
                   java.util.function.Consumer<java.util.Map<String, Object>> callback) {
            try {
                Class<?> customFormClass = manager.loadGeyserClass("org.geysermc.cumulus.form.CustomForm");
                Class<?> customFormBuilderClass = manager.loadGeyserClass("org.geysermc.cumulus.form.CustomForm$Builder");

                // Create builder: CustomForm.builder()
                Method builderMethod = customFormClass.getMethod("builder");
                Object builder = builderMethod.invoke(null);

                // Set title
                Method titleMethod = customFormBuilderClass.getMethod("title", String.class);
                builder = titleMethod.invoke(builder, title);

                // Add components in order
                for (FormComponent comp : components) {
                    switch (comp.type) {
                        case "label":
                            Method labelMethod = customFormBuilderClass.getMethod("label", String.class);
                            builder = labelMethod.invoke(builder, (String) comp.data);
                            break;
                        case "input":
                            Method inputMethod = customFormBuilderClass.getMethod("input",
                                String.class, String.class, String.class);
                            builder = inputMethod.invoke(builder,
                                (String) comp.data,
                                comp.extra != null ? (String) comp.extra : "",
                                comp.defaultVal != null ? (String) comp.defaultVal : "");
                            break;
                        case "dropdown":
                            Method dropdownMethod = customFormBuilderClass.getMethod("dropdown",
                                String.class, java.util.List.class, int.class);
                            @SuppressWarnings("unchecked")
                            java.util.List<String> options = (java.util.List<String>) comp.extra;
                            int defaultIdx = comp.defaultVal != null ? (Integer) comp.defaultVal : 0;
                            builder = dropdownMethod.invoke(builder, (String) comp.data, options, defaultIdx);
                            break;
                        case "toggle":
                            Method toggleMethod = customFormBuilderClass.getMethod("toggle",
                                String.class, boolean.class);
                            boolean toggleDefault = comp.defaultVal != null ? (Boolean) comp.defaultVal : false;
                            builder = toggleMethod.invoke(builder, (String) comp.data, toggleDefault);
                            break;
                        case "slider":
                            Method sliderMethod = customFormBuilderClass.getMethod("slider",
                                String.class, float.class, float.class, float.class, float.class);
                            @SuppressWarnings("unchecked")
                            java.util.List<Number> params = (java.util.List<Number>) comp.extra;
                            float defaultSlider = comp.defaultVal != null ? ((Number) comp.defaultVal).floatValue() : params.get(0).floatValue();
                            builder = sliderMethod.invoke(builder, (String) comp.data,
                                params.get(0).floatValue(), params.get(1).floatValue(),
                                params.get(2).floatValue(), defaultSlider);
                            break;
                    }
                }

                // Set callback handler
                if (callback != null) {
                    Class<?> formResponseClass = manager.loadGeyserClass("org.geysermc.cumulus.response.CustomFormResponse");
                    Class<?> consumerClass = java.util.function.Consumer.class;

                    java.util.function.Consumer<Object> responseHandler = response -> {
                        try {
                            // Check if form was closed
                            Method isClosedMethod = formResponseClass.getMethod("isClosed");
                            Boolean closed = (Boolean) isClosedMethod.invoke(response);

                            if (!closed) {
                                // Get response data
                                Method asMapMethod = formResponseClass.getMethod("asMap");
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> results = (java.util.Map<String, Object>) asMapMethod.invoke(response);

                                // Run callback on main thread
                                manager.plugin.getServer().getScheduler().runTask(manager.plugin,
                                    () -> callback.accept(results));
                            }
                        } catch (Exception e) {
                            manager.plugin.getLogger().log(Level.SEVERE, "Error handling CustomForm response", e);
                        }
                    };

                    // Set valid result handler
                    Method validResultMethod = customFormBuilderClass.getMethod("validResultHandler", consumerClass);
                    builder = validResultMethod.invoke(builder, responseHandler);
                }

                // Build and send form
                Method buildMethod = customFormBuilderClass.getMethod("build");
                Object form = buildMethod.invoke(builder);

                if (!manager.dispatchForm(player, form)) {
                    manager.plugin.getLogger().warning("Failed to send CustomForm to " + player.getName() + " - no compatible sendForm method found");
                }

            } catch (Exception e) {
                manager.plugin.getLogger().log(Level.SEVERE, "Error building/sending CustomForm", e);
            }
        }

        private static class FormComponent {
            final String type;
            final Object data;      // Label text, field name, etc.
            final Object extra;     // Placeholder, options list, slider params
            final Object defaultVal;

            FormComponent(String type, Object data, Object extra, Object defaultVal) {
                this.type = type;
                this.data = data;
                this.extra = extra;
                this.defaultVal = defaultVal;
            }
        }
    }

    /**
     * Create a new CustomForm builder
     */
    public CustomFormBuilder customForm(String title) {
        return new CustomFormBuilder(this, title);
    }

    /**
     * Internal callback wrapper
     */
    private interface FormCallback {
        void handle(Object response);
    }

    private Class<?> loadGeyserClass(String className) throws ClassNotFoundException {
        if (geyserClassLoader != null) {
            return Class.forName(className, true, geyserClassLoader);
        }
        return Class.forName(className);
    }

    private boolean dispatchForm(Player player, Object form) {
        if (form == null) {
            return false;
        }

        try {
            Method connectionMethod = geyserApiClass.getMethod("connectionByUuid", UUID.class);
            Object connection = connectionMethod.invoke(geyserApiInstance, player.getUniqueId());
            if (connection == null) {
                plugin.getLogger().warning("Cannot send form to " + player.getName() + " - no Geyser connection found");
                return false;
            }

            java.util.List<String> inspected = new java.util.ArrayList<>();
            for (Method method : connection.getClass().getMethods()) {
                if (!"sendForm".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> paramType = method.getParameterTypes()[0];
                inspected.add(paramType.getName());

                Object adapted = adaptFormForParameter(form, paramType);
                if (adapted == null) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(connection, adapted);
                    return true;
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.FINE, "sendForm invocation rejected for parameter " + paramType.getName(), ex);
                }
            }

            plugin.getLogger().warning("Failed to invoke sendForm on " + connection.getClass().getName()
                + " for " + player.getName() + " (form=" + form.getClass().getName()
                + "). Tried parameter types: " + inspected);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error dispatching Bedrock form to " + player.getName(), e);
            return false;
        }
    }

    private Object adaptFormForParameter(Object form, Class<?> paramType) {
        if (paramType.isInstance(form)) {
            return form;
        }

        // Handle legacy interface org.geysermc.cumulus.Form
        try {
            Class<?> legacyFormClass = loadGeyserClass("org.geysermc.cumulus.Form");
            if (legacyFormClass.isAssignableFrom(paramType)) {
                Object converted = tryConversionChain(form, legacyFormClass);
                if (converted != null && paramType.isInstance(converted)) {
                    return converted;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Ignore - not present on older builds
        }

        Object converted = tryConversionChain(form, paramType);
        return paramType.isInstance(converted) ? converted : null;
    }

    private Object tryConversionChain(Object form, Class<?> targetType) {
        if (form == null) return null;

        Object result = tryInvokeConversion(form, "newForm");
        if (result != null && targetType.isInstance(result)) {
            return result;
        }

        result = tryInvokeConversion(form, "asForm");
        if (result != null && targetType.isInstance(result)) {
            return result;
        }

        // Some builders expose build/legacyForm methods
        result = tryInvokeConversion(form, "buildLegacy");
        if (result != null && targetType.isInstance(result)) {
            return result;
        }

        return null;
    }

    private Object tryInvokeConversion(Object form, String methodName) {
        try {
            Method method = form.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(form);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Failed to invoke conversion method '" + methodName
                + "' on " + form.getClass().getName(), e);
            return null;
        }
    }
}
