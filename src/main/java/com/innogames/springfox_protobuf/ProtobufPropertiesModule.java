package com.innogames.springfox_protobuf;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Jackson module that works together with com.hubspot.jackson.datatype.protobuf.ProtobufModule.
 * When added to the springfox ObjectMapper, it allows protobuf messages to show up in swagger ui.
 *
 * Copyright (c) 2018 InnoGames GmbH
 */
public class ProtobufPropertiesModule extends Module {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProtobufPropertiesModule.class);

	private Map<Class<?>, Map<String, FieldDescriptor>> cache = new ConcurrentHashMap<>();

	@Override
	public String getModuleName() {
		return "ProtobufPropertyModule";
	}

	@Override
	public Version version() {
		return VersionUtil.packageVersionFor(getClass());
	}


	@Override
	public void setupModule(SetupContext context) {

		context.setClassIntrospector(new ProtobufClassIntrospector());

		context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {

			@Override
			public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac, VisibilityChecker<?> checker) {
				if (Message.class.isAssignableFrom(ac.getRawType())) {
					return checker.withGetterVisibility(Visibility.PUBLIC_ONLY);
				}
				return super.findAutoDetectVisibility(ac, checker);
			}
		});

	}


	class ProtobufClassIntrospector extends BasicClassIntrospector {

		@Override
		public BasicBeanDescription forDeserialization(DeserializationConfig cfg, JavaType type, MixInResolver r) {
			BasicBeanDescription desc = super.forDeserialization(cfg, type, r);

			if (Message.class.isAssignableFrom(type.getRawClass())) {
				return protobufBeanDescription(cfg, type, r, desc);
			}

			return desc;
		}


		@Override
		public BasicBeanDescription forSerialization(SerializationConfig cfg, JavaType type, MixInResolver r) {
			BasicBeanDescription desc = super.forSerialization(cfg, type, r);

			if (Message.class.isAssignableFrom(type.getRawClass())) {
				return protobufBeanDescription(cfg, type, r, desc);
			}

			return desc;
		}

		private BasicBeanDescription protobufBeanDescription(MapperConfig<?> cfg, JavaType type, MixInResolver r, BasicBeanDescription baseDesc) {

			Map<String, FieldDescriptor> types = cache.computeIfAbsent(type.getRawClass(), this::getDescriptorForType);

			AnnotatedClass ac = AnnotatedClassResolver.resolve(cfg, type, r);

			List<BeanPropertyDefinition> props = new ArrayList<>();

			for (BeanPropertyDefinition p : baseDesc.findProperties()) {
				String name = p.getName();

				if (types.containsKey(name)) {
					props.add(p.withSimpleName(name));
					continue;
				}

				// special handler for Lists
				if (name.endsWith("List") && types.containsKey(substr(name, 4))) {
					props.add(p.withSimpleName(substr(name, 4)));
				}

			}

			return new BasicBeanDescription(cfg, type, ac, props) {};
		}


		private Map<String, FieldDescriptor> getDescriptorForType(Class<?> type) {
			try {
				Descriptor invoke = (Descriptor) type.getMethod("getDescriptor").invoke(null);

				return invoke.getFields().stream()
					.collect(toMap(FieldDescriptor::getName, Function.identity()));

			} catch (Exception e) {

				log.error("Error getting protobuf descriptor for swagger.", e);
				return new HashMap<>();
			}
		}


		private String substr(String name, int cnt) {
			return name.substring(0, name.length() - cnt);
		}

	}
}
