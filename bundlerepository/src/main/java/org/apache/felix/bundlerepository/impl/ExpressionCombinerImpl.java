/*
 * Copyright (c) OSGi Alliance (2011, 2018). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.bundlerepository.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Requirement;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.RequirementExpression;

public class ExpressionCombinerImpl implements ExpressionCombiner {

    protected abstract class AbstractExpressionImpl {
        List<RequirementExpression> expressions = new ArrayList<>();

        @Override
        public String toString() {
            final int maxLen = 10;
            return "AbstractExpressionImpl [expressions="
                    + (expressions != null
                            ? expressions.subList(0,
                                    Math.min(expressions.size(), maxLen))
                            : null)
                    + "]";
        }
    }

    protected class AndExpressionImpl extends AbstractExpressionImpl
            implements AndExpression {

        public AndExpressionImpl(RequirementExpression pRequirementExpression1,
                RequirementExpression pRequirementExpression2) {
            expressions.add(pRequirementExpression1);
            expressions.add(pRequirementExpression2);
        }

        public AndExpressionImpl(RequirementExpression pRequirementExpression1,
                RequirementExpression pRequirementExpression2,
                RequirementExpression[] pMoreExprs) {
            this(pRequirementExpression1, pRequirementExpression2);
            for (RequirementExpression requirementExpression : pMoreExprs) {
                expressions.add(requirementExpression);
            }
        }

        @Override
        public List<RequirementExpression> getRequirementExpressions() {
            return expressions;
        }
    }

    protected class NotExpressionImpl extends AbstractExpressionImpl
            implements NotExpression {

        public NotExpressionImpl(RequirementExpression pRequirementExpression) {
            expressions.add(pRequirementExpression);
        }

        @Override
        public RequirementExpression getRequirementExpression() {
            return expressions.iterator().next();
        }

    }

    protected class OrExpressionImpl extends AbstractExpressionImpl
            implements OrExpression {

        public OrExpressionImpl(RequirementExpression pRequirementExpression1,
                RequirementExpression pRequirementExpression2) {
            expressions.add(pRequirementExpression1);
            expressions.add(pRequirementExpression2);
        }

        public OrExpressionImpl(RequirementExpression pExpr1,
                RequirementExpression pExpr2,
                RequirementExpression[] pMoreExprs) {
            this(pExpr1, pExpr2);
            for (RequirementExpression requirementExpression : pMoreExprs) {
                expressions.add(requirementExpression);
            }
        }

        @Override
        public List<RequirementExpression> getRequirementExpressions() {
            return expressions;
        }
    }

    @Override
    public AndExpression and(RequirementExpression pExpr1,
            RequirementExpression pExpr2) {
        return new AndExpressionImpl(pExpr1, pExpr2);
    }

    @Override
    public AndExpression and(RequirementExpression pExpr1,
            RequirementExpression pExpr2, RequirementExpression... pMoreExprs) {
        return new AndExpressionImpl(pExpr1, pExpr2, pMoreExprs);
    }

    @Override
    public IdentityExpression identity(Requirement pReq) {
        return new IdentityExpressionImpl(pReq);
    }

    @Override
    public NotExpression not(RequirementExpression pExpr) {
        return new NotExpressionImpl(pExpr);
    }

    @Override
    public OrExpression or(RequirementExpression pExpr1,
            RequirementExpression pExpr2) {
        return new OrExpressionImpl(pExpr1, pExpr2);
    }

    @Override
    public OrExpression or(RequirementExpression pExpr1,
            RequirementExpression pExpr2, RequirementExpression... pMoreExprs) {
        return new OrExpressionImpl(pExpr1, pExpr2, pMoreExprs);
    }

}
