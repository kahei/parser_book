\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{asciibook}[2026/08/23 parser_book preview compatibility class]

% This is intentionally not a reimplementation of the production class.
% It provides only the interfaces used by this repository's publish sources.
\LoadClassWithOptions{ltjsbook}

\makeatletter
\@ifundefined{ps@frontheadings}{\let\ps@frontheadings\ps@plain}{}
\@ifundefined{ps@asciiheadings}{\let\ps@asciiheadings\ps@headings}{}
\makeatother

\endinput
