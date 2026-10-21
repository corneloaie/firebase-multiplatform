"""A small reader for the Kotlin sources of the Firebase Android SDK's pipeline API.

It is not a Kotlin parser: it relies on the layout ktfmt gives those files (members on their own lines, continuation lines
indented deeper than the member they continue), which is enough to pick out the public functions of a class and their
bodies.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field


def strip_comments(text: str) -> str:
    """Removes `//` and `/* */` comments (KDoc included) outside string and character literals; keeps the newlines."""
    out = []
    i, n = 0, len(text)
    while i < n:
        if text.startswith('"""', i):
            end = text.find('"""', i + 3)
            end = n if end < 0 else end + 3
            out.append(text[i:end])
            i = end
        elif text[i] == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == '\\' else 1
            out.append(text[i:j + 1])
            i = j + 1
        elif text[i] == "'":
            j = i + 1
            while j < n and text[j] != "'":
                j += 2 if text[j] == '\\' else 1
            out.append(text[i:j + 1])
            i = j + 1
        elif text.startswith('//', i):
            end = text.find('\n', i)
            i = n if end < 0 else end
        elif text.startswith('/*', i):
            end = text.find('*/', i + 2)
            end = n if end < 0 else end + 2
            out.append('\n' * text.count('\n', i, end))
            i = end
        else:
            out.append(text[i])
            i += 1
    return ''.join(out)


_OPEN = {'(': ')', '[': ']', '{': '}', '<': '>'}


def matching(text: str, start: int) -> int:
    """The index just past the bracket closing the one at [start], skipping string literals."""
    opening = text[start]
    closing = _OPEN[opening]
    depth, i = 0, start
    while i < len(text):
        c = text[i]
        if c == '"':
            j = i + 1
            while text[j] != '"':
                j += 2 if text[j] == '\\' else 1
            i = j + 1
            continue
        if c == opening:
            depth += 1
        elif c == closing:
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise ValueError(f'unbalanced {opening} at {start}')


def split_top_level(text: str, separator: str = ',') -> list[str]:
    """Splits [text] at the separators that are not nested in brackets or string literals."""
    parts, depth, current, i = [], 0, [], 0
    while i < len(text):
        c = text[i]
        if c == '"':
            j = i + 1
            while text[j] != '"':
                j += 2 if text[j] == '\\' else 1
            current.append(text[i:j + 1])
            i = j + 1
            continue
        if c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
        elif c == '<' and re.match(r'<[\w.?*, <>]*>', text[i:]):
            # a type argument list, as in List<Any>: kept whole
            end = matching(text, i)
            current.append(text[i:end])
            i = end
            continue
        if c == separator and depth == 0:
            parts.append(''.join(current).strip())
            current = []
        else:
            current.append(c)
        i += 1
    tail = ''.join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def class_body(text: str, header: str) -> tuple[str, int]:
    """The text between the braces of the first declaration matching the regex [header], and the column of the header."""
    match = re.search(header, text, re.M)
    if not match:
        raise ValueError(f'no declaration matches {header}')
    brace = text.index('{', match.end() - 1)
    end = matching(text, brace)
    line_start = text.rfind('\n', 0, match.start()) + 1
    indent = len(text[line_start:]) - len(text[line_start:].lstrip(' '))
    return text[brace + 1:end - 1], indent


@dataclass
class Parameter:
    name: str
    type: str
    vararg: bool = False
    default: str | None = None
    annotations: list[str] = field(default_factory=list)


@dataclass
class Function:
    name: str
    annotations: list[str]
    modifiers: list[str]
    parameters: list[Parameter]
    return_type: str | None
    body: str
    expression_body: bool

    @property
    def is_public(self) -> bool:
        return not {'private', 'internal', 'protected'} & set(self.modifiers)

    def signature_key(self) -> tuple:
        return self.name, tuple(('vararg ' if p.vararg else '') + p.type for p in self.parameters)


_DECLARATION = re.compile(r'^(?P<indent> *)(?P<head>(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|private|internal|protected|override|open|final|abstract|inline|operator|infix|suspend|tailrec)\s+)*)fun\s+(?:<[^>]*>\s*)?(?P<name>`?\w+`?)\s*\(', re.M)
_ANNOTATION_LINE = re.compile(r'^ *@\w+(?:\([^)]*\))?\s*$')


def parse_parameters(text: str) -> list[Parameter]:
    parameters = []
    for part in split_top_level(text):
        if not part:
            continue
        annotations = re.findall(r'@\w+(?:\([^)]*\))?', part)
        part = re.sub(r'@\w+(?:\([^)]*\))?\s*', '', part).strip()
        vararg = part.startswith('vararg ')
        part = part.removeprefix('vararg ').strip()
        default = None
        pieces = split_top_level(part, '=')
        if len(pieces) == 2:
            part, default = pieces
        name, _, type_ = part.partition(':')
        parameters.append(Parameter(name.strip(), ' '.join(type_.split()), vararg, default, annotations))
    return parameters


def functions(body: str, indent: int) -> list[Function]:
    """The functions declared directly in a class [body] whose members are indented by [indent] spaces."""
    result = []
    lines = body.split('\n')
    offsets = []
    position = 0
    for line in lines:
        offsets.append(position)
        position += len(line) + 1
    for match in _DECLARATION.finditer(body):
        if len(match.group('indent')) != indent:
            continue
        # annotations on the lines above (`@JvmStatic` then the declaration)
        line_index = body.count('\n', 0, match.start())
        annotations = re.findall(r'@\w+(?:\([^)]*\))?', match.group('head'))
        k = line_index - 1
        while k >= 0 and _ANNOTATION_LINE.match(lines[k]):
            annotations = re.findall(r'@\w+(?:\([^)]*\))?', lines[k]) + annotations
            k -= 1
        modifiers = re.findall(r'\b(public|private|internal|protected|override|open|final|abstract|inline|operator|infix)\b', re.sub(r'@\w+(?:\([^)]*\))?', '', match.group('head')))
        open_paren = match.end() - 1
        close = matching(body, open_paren)
        parameters = parse_parameters(body[open_paren + 1:close - 1])
        rest_start = close
        if 'abstract' in modifiers:
            line_end = body.find('\n', rest_start)
            return_part = body[rest_start:len(body) if line_end < 0 else line_end].strip()
            return_type = ' '.join(return_part[1:].split()) if return_part.startswith(':') else None
            result.append(Function(match.group('name').strip('`'), annotations, modifiers, parameters, return_type, '', False))
            continue
        # the return type runs to the `=` or `{` that starts the body, at bracket depth 0
        i, depth = rest_start, 0
        while i < len(body):
            c = body[i]
            if c in '(<[':
                depth += 1
            elif c in ')>]':
                depth -= 1
            elif depth == 0 and (c == '{' or (c == '=' and body[i + 1] != '=')):
                break
            elif c == '\n' and depth == 0 and body[rest_start:i].strip() and not body[rest_start:i].strip().startswith(':'):
                break
            i += 1
        return_part = body[rest_start:i].strip()
        return_type = ' '.join(return_part[1:].split()) if return_part.startswith(':') else None
        if i >= len(body) or body[i] not in '{=':
            # abstract or bodiless
            result.append(Function(match.group('name').strip('`'), annotations, modifiers, parameters, return_type, '', False))
            continue
        if body[i] == '{':
            end = matching(body, i)
            text = body[i + 1:end - 1]
            expression_body = False
        else:
            # an expression body continues on the lines indented deeper than the declaration
            j = body.find('\n', i)
            end = len(body) if j < 0 else j
            while j >= 0:
                next_newline = body.find('\n', j + 1)
                line = body[j + 1:len(body) if next_newline < 0 else next_newline]
                if line.strip() and (len(line) - len(line.lstrip(' '))) <= indent:
                    break
                end = len(body) if next_newline < 0 else next_newline
                j = next_newline
            text = body[i + 1:end]
            expression_body = True
        result.append(Function(match.group('name').strip('`'), annotations, modifiers, parameters, return_type, ' '.join(text.split()), expression_body))
    return result
