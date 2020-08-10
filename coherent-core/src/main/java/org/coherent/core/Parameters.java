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

import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;

public final class Parameters {
	Parameters() {}

	@SafeVarargs public static <S, C, A> Parameter<S, C, A> parameterOption(String name, String description, Tuple<String, A>... options) {
		return parameter(
			name,
			description,
			Arrays.stream(options)
				.reduce(ignore(), (parser, option) -> parser.plus(replace(string(option.first()), option.second())), Parser::plus),
			completer(Arrays.stream(options).map(option -> text(option.first())).toArray(Text[]::new))
		);
	}

	public static <S, C> Parameter<S, C, String> parameterLiteral(String name, String description, String... literals) {
		return parameter(
			name,
			description,
			Arrays.stream(literals)
				.reduce(ignore(), (parser, literal) -> parser.plus(string(literal)), Parser::plus),
			completer(Arrays.stream(literals).map(literal -> text(literal)).toArray(Text[]::new))
		);
	}

	public static <S, C> Parameter<S, C, String> parameterPhrase(String name, String description) {
		return parameter(
			name,
			description,
			stringDissatisfy(Character::isWhitespace),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Boolean> parameterBoolean(String name, String description) {
		return parameter(
			name,
			description,
			readBoolean(),
			completer(text("false"), text("true"))
		);
	}

	public static <S, C> Parameter<S, C, Byte> parameterByte(String name, String description) {
		return parameter(
			name,
			description,
			readByte(),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Short> parameterShort(String name, String description) {
		return parameter(
			name,
			description,
			readShort(),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Integer> parameterInteger(String name, String description) {
		return parameter(
			name,
			description,
			readInteger(),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Long> parameterLong(String name, String description) {
		return parameter(
			name,
			description,
			readLong(),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Float> parameterFloat(String name, String description) {
		return parameter(
			name,
			description,
			readFloat(),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Double> parameterDouble(String name, String description) {
		return parameter(
			name,
			description,
			readDouble(),
			incompletable()
		);
	}

	public static <S, C> Parameter<S, C, Character> parameterCharacter(String name, String description) {
		return parameter(
			name,
			description,
			readCharacter(),
			completer(text("'"))
		);
	}

	public static <S, C> Parameter<S, C, String> parameterString(String name, String description) {
		return parameter(
			name,
			description,
			readString(),
			completer(text("\""))
		);
	}
}
