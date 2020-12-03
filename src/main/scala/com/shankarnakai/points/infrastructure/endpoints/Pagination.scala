package com.shankarnakai.points.infrastructure.endpoints

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object Pagination {
  import QueryParamDecoder._

  object OptionalPageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("pageSize")
  object OptionalOffsetMatcher extends OptionalQueryParamDecoderMatcher[Int]("offset")
}
