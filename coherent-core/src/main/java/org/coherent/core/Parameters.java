package org.coherent.core;

import java.util.Arrays;

import org.coherent.core.Command.Parameter;
import static org.coherent.core.Command.Parameter.*;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;
import org.jparsec.core.parser.Read;
import static org.jparsec.core.parser.Read.*;

import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;

public final class Parameters {
	Parameters() {}

	@SafeVarargs public static <S, C, A> Parameter<S, C, A> parameterUnion(String name, String description, Parameter<S, C, A>... parameters) {
		return parameter(
			name,
			description,
			Arrays.stream(parameters)
				.reduce(ignore(), (parser, parameter) -> parser.plus(parameter.parser()), Parser::plus),
			(input, context) -> list(Arrays.stream(parameters)
				.flatMap(parameter -> parameter.completer().apply(input, context).stream())
				.distinct()
				.toArray(Text[]::new)
			)
		);
	}

	@SafeVarargs public static <S, C, A> Parameter<S, C, A> parameterOption(String name, String description, Tuple<String, A>... options) {
		return parameter(
			name,
			description,
			Arrays.stream(options)
				.reduce(ignore(), (parser, option) -> parser.plus(replace(string(option.first()), option.second())), Parser::plus),
			(input, context) -> list(Arrays.stream(options)
				.map(option -> text(option.first()))
				.filter(completion -> input.isPrefixOf(completion))
				.distinct()
				.toArray(Text[]::new)
			)
		);
	}

	public static <S, C> Parameter<S, C, String> parameterLiteral(String name, String description, String... literals) {
		return parameter(
			name,
			description,
			Arrays.stream(literals)
				.reduce(ignore(), (parser, literal) -> parser.plus(string(literal)), Parser::plus),
			(input, context) -> list(Arrays.stream(literals)
				.map(literal -> text(literal))
				.filter(completion -> input.isPrefixOf(completion))
				.distinct()
				.toArray(Text[]::new)
			)
		);
	}

	public static <S, C> Parameter<S, C, String> parameterPhrase(String name, String description) {
		return parameter(
			name,
			description,
			stringDissatisfy(Character::isWhitespace),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Boolean> parameterBoolean(String name, String description) {
		return parameter(
			name,
			description,
			readBoolean(),
			(input, context) -> list(text("false"), text("true")).filter(completion -> input.isPrefixOf(completion))
		);
	}

	public static <S, C> Parameter<S, C, Byte> parameterByte(String name, String description) {
		return parameter(
			name,
			description,
			readByte(),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Short> parameterShort(String name, String description) {
		return parameter(
			name,
			description,
			readShort(),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Integer> parameterInteger(String name, String description) {
		return parameter(
			name,
			description,
			readInteger(),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Long> parameterLong(String name, String description) {
		return parameter(
			name,
			description,
			readLong(),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Float> parameterFloat(String name, String description) {
		return parameter(
			name,
			description,
			readFloat(),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Double> parameterDouble(String name, String description) {
		return parameter(
			name,
			description,
			readDouble(),
			(input, context) -> nil()
		);
	}

	public static <S, C> Parameter<S, C, Character> parameterCharacter(String name, String description) {
		return parameter(
			name,
			description,
			readCharacter(),
			(input, context) -> input.isPrefixOf(text("'")) ? singleton(text("'")) : nil()
		);
	}

	public static <S, C> Parameter<S, C, String> parameterString(String name, String description) {
		return parameter(
			name,
			description,
			readString(),
			(input, context) -> input.isPrefixOf(text("\"")) ? singleton(text("\"")) : nil()
		);
	}
}
