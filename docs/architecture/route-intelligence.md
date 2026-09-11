# Route Intelligence

The route layer distinguishes syntax from observed endpoint identity.

Supported syntax classes:

- OBSERVED
- BRACE_TEMPLATE, for example `/users/{id}`
- COLON_TEMPLATE, for example `/users/:id`
- ANGLE_TEMPLATE, for example `/users/<int:id>`
- LITERAL_WILDCARD
- CATCH_ALL
- REGEX-like

Equivalence states:

- SYNTACTICALLY_EQUAL
- CANONICALLY_EQUIVALENT
- SAME_FAMILY
- DIFFERENT
- UNKNOWN

Route equivalence is a reconnaissance observation. It does not claim that two representations traverse the same proxy, middleware or authorization boundary at runtime.
