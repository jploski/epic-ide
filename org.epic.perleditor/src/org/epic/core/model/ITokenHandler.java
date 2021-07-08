package org.epic.core.model;

import java.util.List;

import org.epic.core.parser.PerlToken;

public interface ITokenHandler
{

    void start(SourceFile sourceFile, List<PerlToken> tokens);
    void end();
    void handleToken(int currentTokenIdx);

}
